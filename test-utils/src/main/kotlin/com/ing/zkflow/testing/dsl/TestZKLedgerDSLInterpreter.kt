
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.isZKFlowTransaction
import com.ing.zkflow.testing.dsl.AttachmentResolutionException
import com.ing.zkflow.testing.dsl.DoubleSpentInputs
import com.ing.zkflow.testing.dsl.DuplicateOutputLabel
import com.ing.zkflow.testing.dsl.EnforceVerifyOrFail
import com.ing.zkflow.testing.dsl.LedgerDSLInterpreter
import com.ing.zkflow.testing.dsl.TestDSLZKTransactionService
import com.ing.zkflow.testing.dsl.TestZKTransactionDSLInterpreter
import com.ing.zkflow.testing.dsl.VerificationMode
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.NullKeys
import net.corda.core.crypto.SecureHash
import net.corda.core.internal.uncheckedCast
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.dummyCommand
import java.io.InputStream

/**
 *  A ledger interpreter that supports both standard and zk transactions. The appropriate transaction DSL interpreter is called
 *  depending on the type of transaction.
 */
@Suppress("TooManyFunctions")
public data class TestZKLedgerDSLInterpreter private constructor(
    val services: ServiceHub,
    internal val labelToOutputStateAndRefs: HashMap<String, StateAndRef<ContractState>> = HashMap(),
    private val transactionWithLocations: HashMap<SecureHash, WireTransactionWithLocation> = LinkedHashMap(),
    private val nonVerifiedTransactionWithLocations: HashMap<SecureHash, WireTransactionWithLocation> = HashMap(),
    val zkService: TestDSLZKTransactionService,
    val serializationSchemeID: Int
) : LedgerDSLInterpreter<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter> {
    val wireTransactions: List<WireTransaction> get() = transactionWithLocations.values.map { it.transaction }

    // We specify [labelToOutputStateAndRefs] just so that Kotlin picks the primary constructor instead of cycling
    public constructor(services: ServiceHub, zkService: TestDSLZKTransactionService, serializationSchemeID: Int) : this(
        services,
        labelToOutputStateAndRefs = HashMap(),
        zkService = zkService,
        serializationSchemeID = serializationSchemeID
    )

    public companion object {
        private fun getCallerLocation(): String? {
            val stackTrace = Thread.currentThread().stackTrace
            for (i in 1..stackTrace.size) {
                val stackTraceElement = stackTrace[i]
                if (!stackTraceElement.fileName.contains("DSL")) {
                    return stackTraceElement.toString()
                }
            }
            return null
        }
    }

    internal data class WireTransactionWithLocation(
        val label: String?,
        val transaction: WireTransaction,
        val location: String?
    )

    public class VerifiesFailed(transactionName: String, cause: Throwable) :
        Exception("Transaction ($transactionName) didn't verify: $cause", cause)

    public class TypeMismatch(requested: Class<*>, actual: Class<*>) :
        Exception("Actual type $actual is not a subtype of requested type $requested")

    internal fun copy(): TestZKLedgerDSLInterpreter =
        TestZKLedgerDSLInterpreter(
            services,
            labelToOutputStateAndRefs = HashMap(labelToOutputStateAndRefs),
            transactionWithLocations = HashMap(transactionWithLocations),
            nonVerifiedTransactionWithLocations = HashMap(nonVerifiedTransactionWithLocations),
            zkService = zkService,
            serializationSchemeID = serializationSchemeID
        )

    internal fun getTransaction(id: SecureHash): SignedTransaction? {
        val tx = transactionWithLocations[id] ?: nonVerifiedTransactionWithLocations[id]
        return tx?.let { SignedTransaction(it.transaction, listOf(NullKeys.NULL_SIGNATURE)) }
    }

    internal inline fun <reified S : ContractState> resolveStateRef(stateRef: StateRef): TransactionState<S> {
        val transactionWithLocation =
            transactionWithLocations[stateRef.txhash] ?: nonVerifiedTransactionWithLocations[stateRef.txhash]
                ?: throw TransactionResolutionException(stateRef.txhash)
        val output = transactionWithLocation.transaction.outputs[stateRef.index]
        return if (S::class.java.isAssignableFrom(output.data.javaClass)) {
            uncheckedCast(output)
        } else {
            throw TypeMismatch(requested = S::class.java, actual = output.data.javaClass)
        }
    }

    internal fun resolveAttachment(attachmentId: SecureHash): Attachment {
        return services.attachments.openAttachment(attachmentId) ?: throw AttachmentResolutionException(attachmentId)
    }

    private fun <R> interpretTransactionDsl(
        transactionBuilder: TransactionBuilder,
        dsl: TestTransactionDSLInterpreter.() -> R
    ): TestTransactionDSLInterpreter {
        return TestTransactionDSLInterpreter(this, transactionBuilder).apply { dsl() }
    }

    private fun <R> interpretTransactionDsl(
        transactionBuilder: ZKTransactionBuilder,
        dsl: TestZKTransactionDSLInterpreter.() -> R
    ): TestZKTransactionDSLInterpreter {
        return TestZKTransactionDSLInterpreter(this, transactionBuilder).apply { dsl() }
    }

    public fun transactionName(transactionHash: SecureHash): String? {
        val transactionWithLocation = transactionWithLocations[transactionHash]
        return if (transactionWithLocation != null) {
            transactionWithLocation.label ?: "TX[${transactionWithLocation.location}]"
        } else {
            null
        }
    }

    public fun outputToLabel(state: ContractState): String? =
        labelToOutputStateAndRefs.filter { it.value.state.data == state }.keys.firstOrNull()

    private fun <R> recordZKTransactionWithTransactionMap(
        transactionLabel: String?,
        transactionBuilder: ZKTransactionBuilder,
        dsl: TestZKTransactionDSLInterpreter.() -> R,
        transactionMap: HashMap<SecureHash, WireTransactionWithLocation> = HashMap(),
        /** If set to true, will add dummy components to [transactionBuilder] to make it valid. */
        fillTransaction: Boolean
    ): WireTransaction {
        val transactionLocation = getCallerLocation()
        val transactionInterpreter = interpretTransactionDsl(transactionBuilder, dsl)
        if (fillTransaction) fillTransaction(transactionBuilder)
        // Create the WireTransaction
        val wireTransaction = try {
            transactionInterpreter.toWireTransaction()
        } catch (e: IllegalStateException) {
            throw IllegalStateException("A transaction-DSL block that is part of a test ledger must return a valid transaction.", e)
        }
        // Record the output states
        transactionInterpreter.labelToIndexMap.forEach { label, index ->
            if (label in labelToOutputStateAndRefs) {
                throw DuplicateOutputLabel(label)
            }
            labelToOutputStateAndRefs[label] = wireTransaction.outRef(index)
        }
        transactionMap[wireTransaction.id] =
            WireTransactionWithLocation(transactionLabel, wireTransaction, transactionLocation)

        return wireTransaction
    }

    private fun <R> recordTransactionWithTransactionMap(
        transactionLabel: String?,
        transactionBuilder: TransactionBuilder,
        dsl: TestTransactionDSLInterpreter.() -> R,
        transactionMap: HashMap<SecureHash, WireTransactionWithLocation> = HashMap(),
        /** If set to true, will add dummy components to [transactionBuilder] to make it valid. */
        fillTransaction: Boolean
    ): WireTransaction {
        val transactionLocation = getCallerLocation()
        val transactionInterpreter = interpretTransactionDsl(transactionBuilder, dsl)
        if (fillTransaction) fillTransaction(transactionBuilder)
        // Create the WireTransaction
        val wireTransaction = try {
            transactionInterpreter.toWireTransaction()
        } catch (e: IllegalStateException) {
            throw IllegalStateException("A transaction-DSL block that is part of a test ledger must return a valid transaction.", e)
        }
        // Record the output states
        transactionInterpreter.labelToIndexMap.forEach { label, index ->
            if (label in labelToOutputStateAndRefs) {
                throw DuplicateOutputLabel(label)
            }
            labelToOutputStateAndRefs[label] = wireTransaction.outRef(index)
        }
        transactionMap[wireTransaction.id] =
            WireTransactionWithLocation(transactionLabel, wireTransaction, transactionLocation)

        return wireTransaction
    }

    /**
     * This method fills the transaction builder with dummy components to satisfy the base transaction validity rules.
     *
     * A common pattern in our tests is using a base transaction and expressing the test cases using [tweak]s.
     * The base transaction may not be valid, but it still gets recorded to the ledger. This causes a test failure,
     * even though is not being used for anything afterwards.
     */
    private fun fillTransaction(transactionBuilder: TransactionBuilder) {
        if (transactionBuilder.commands().isEmpty()) transactionBuilder.addCommand(dummyCommand())
    }

    /**
     * This method fills the transaction builder with dummy components to satisfy the base transaction validity rules.
     *
     * A common pattern in our tests is using a base transaction and expressing the test cases using [tweak]s.
     * The base transaction may not be valid, but it still gets recorded to the ledger. This causes a test failure,
     * even though is not being used for anything afterwards.
     */
    private fun fillTransaction(transactionBuilder: ZKTransactionBuilder) {
        if (transactionBuilder.commands().isEmpty()) transactionBuilder.addCommand(dummyCommand())
    }

    override fun _transaction(
        transactionLabel: String?,
        transactionBuilder: TransactionBuilder,
        dsl: TestTransactionDSLInterpreter.() -> EnforceVerifyOrFail
    ): WireTransaction = recordTransactionWithTransactionMap(transactionLabel, transactionBuilder, dsl, transactionWithLocations, false)

    override fun _zkTransaction(
        transactionLabel: String?,
        transactionBuilder: ZKTransactionBuilder,
        dsl: TestZKTransactionDSLInterpreter.() -> EnforceVerifyOrFail
    ): WireTransaction = recordZKTransactionWithTransactionMap(transactionLabel, transactionBuilder, dsl, transactionWithLocations, false)

    override fun _unverifiedTransaction(
        transactionLabel: String?,
        transactionBuilder: TransactionBuilder,
        dsl: TestTransactionDSLInterpreter.() -> Unit
    ): WireTransaction =
        recordTransactionWithTransactionMap(transactionLabel, transactionBuilder, dsl, nonVerifiedTransactionWithLocations, true)

    override fun _tweak(dsl: LedgerDSLInterpreter<TestTransactionDSLInterpreter, TestZKTransactionDSLInterpreter>.() -> Unit): Unit =
        copy().dsl()

    override fun attachment(attachment: InputStream): SecureHash {
        return services.attachments.importAttachment(attachment, "TestDSL", null)
    }

    override fun verifies(mode: VerificationMode): EnforceVerifyOrFail {
        try {
            val usedInputs = mutableSetOf<StateRef>()
            services.recordTransactions(transactionsUnverified.map { SignedTransaction(it, listOf(NullKeys.NULL_SIGNATURE)) })
            for ((_, value) in transactionWithLocations) {
                val wtx = value.transaction
                val ltx = wtx.toLedgerTransaction(services)
                ltx.verify()
                if (wtx.isZKFlowTransaction) {
                    zkService.verify(wtx, mode)
                }
                val allInputs = wtx.inputs union wtx.references
                val doubleSpend = allInputs intersect usedInputs
                if (!doubleSpend.isEmpty()) {
                    val txIds = mutableListOf(wtx.id)
                    doubleSpend.mapTo(txIds) { it.txhash }
                    throw DoubleSpentInputs(txIds)
                }
                usedInputs.addAll(wtx.inputs)
                services.recordTransactions(SignedTransaction(wtx, listOf(NullKeys.NULL_SIGNATURE)))
            }
            return EnforceVerifyOrFail.Token
        } catch (exception: TransactionVerificationException) {
            val transactionWithLocation = transactionWithLocations[exception.txId]
            val transactionName = transactionWithLocation?.label ?: transactionWithLocation?.location ?: "<unknown>"
            throw VerifiesFailed(transactionName, exception)
        }
    }

    override fun <S : ContractState> retrieveOutputStateAndRef(clazz: Class<S>, label: String): StateAndRef<S> {
        val stateAndRef = labelToOutputStateAndRefs[label]
        if (stateAndRef == null) {
            throw IllegalArgumentException("State with label '$label' was not found")
        } else if (!clazz.isAssignableFrom(stateAndRef.state.data.javaClass)) {
            throw TypeMismatch(requested = clazz, actual = stateAndRef.state.data.javaClass)
        } else {
            return uncheckedCast(stateAndRef)
        }
    }

    val transactionsToVerify: List<WireTransaction> get() = transactionWithLocations.values.map { it.transaction }
    val transactionsUnverified: List<WireTransaction> get() = nonVerifiedTransactionWithLocations.values.map { it.transaction }
}
