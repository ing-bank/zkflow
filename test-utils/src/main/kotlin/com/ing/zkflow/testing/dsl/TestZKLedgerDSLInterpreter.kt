
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.transactions.ZKVerifierTransaction
import com.ing.zkflow.common.transactions.zkVerify
import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.testing.dsl.TestZKTransactionDSLInterpreter
import com.ing.zkflow.testing.dsl.interfaces.AttachmentResolutionException
import com.ing.zkflow.testing.dsl.interfaces.DoubleSpentInputs
import com.ing.zkflow.testing.dsl.interfaces.DuplicateOutputLabel
import com.ing.zkflow.testing.dsl.interfaces.EnforceVerifyOrFail
import com.ing.zkflow.testing.dsl.interfaces.ZKLedgerDSLInterpreter
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.NullKeys
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.internal.uncheckedCast
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.dummyCommand
import java.io.InputStream

/**
 * Set up a ledger context. Please note that if you change one of the zkService or zkVerifierTransactionStorage parameters, you
 * should also change the other: zkService should always use ZKVerifierTransactionStorage from the zkVerifierTransactionStorage parameter.
 * If it uses another one, there will be transaction resolution errors.
 */
@Suppress("LongParameterList")
public class TestZKLedgerDSLInterpreter private constructor(
    public val services: ServiceHub,
    private val labelToOutputStateAndRefs: HashMap<String, StateAndRef<ContractState>> = HashMap(),
    private val transactionWithLocations: HashMap<SecureHash, WireTransactionWithLocation> = LinkedHashMap(),
    private val nonVerifiedTransactionWithLocations: HashMap<SecureHash, WireTransactionWithLocation> = HashMap(),
    public override val zkService: ZKTransactionService,
    override val zkNetworkParameters: ZKNetworkParameters,
    override val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage
) : ZKLedgerDSLInterpreter<TestZKTransactionDSLInterpreter> {
    // We specify [labelToOutputStateAndRefs] just so that Kotlin picks the primary constructor instead of cycling
    public constructor(
        services: ServiceHub,
        zkService: ZKTransactionService,
        zkNetworkParameters: ZKNetworkParameters,
        zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage
    ) : this(
        services,
        labelToOutputStateAndRefs = HashMap(),
        zkService = zkService,
        zkNetworkParameters = zkNetworkParameters,
        zkVerifierTransactionStorage = zkVerifierTransactionStorage
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
            zkNetworkParameters = zkNetworkParameters,
            zkVerifierTransactionStorage = zkVerifierTransactionStorage
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

        /*
         * We can use fake/empty proofs here when storing the zkvtx.
         * This is because we never verify them for old txs in the DSL: we create and verify proofs when creating txs.
         */
        zkVerifierTransactionStorage.addTransaction(
            SignedZKVerifierTransaction(ZKVerifierTransaction.fromWireTransaction(wireTransaction, emptyMap()))
        )

        return wireTransaction
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

    public override fun _transaction(
        transactionLabel: String?,
        transactionBuilder: ZKTransactionBuilder,
        dsl: TestZKTransactionDSLInterpreter.() -> EnforceVerifyOrFail
    ): WireTransaction = recordZKTransactionWithTransactionMap(transactionLabel, transactionBuilder, dsl, transactionWithLocations, false)

    override fun _unverifiedTransaction(
        transactionLabel: String?,
        transactionBuilder: ZKTransactionBuilder,
        dsl: TestZKTransactionDSLInterpreter.() -> Unit
    ): WireTransaction =
        recordZKTransactionWithTransactionMap(transactionLabel, transactionBuilder, dsl, nonVerifiedTransactionWithLocations, true)

    override fun _tweak(dsl: ZKLedgerDSLInterpreter<TestZKTransactionDSLInterpreter>.() -> Unit): Unit =
        copy().dsl()

    override fun attachment(attachment: InputStream): SecureHash {
        return services.attachments.importAttachment(attachment, "TestDSL", null)
    }

    override fun verifies(): EnforceVerifyOrFail {
        try {
            val usedInputs = mutableSetOf<StateRef>()
            services.recordTransactions(transactionsUnverified.map { SignedTransaction(it, listOf(NullKeys.NULL_SIGNATURE)) })
            for ((_, value) in transactionWithLocations) {
                val wtx = value.transaction
                val stx = services.signInitialTransaction(wtx)
                stx.zkVerify(services, zkService, zkVerifierTransactionStorage, checkSufficientSignatures = false)

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

    private val transactionsUnverified: List<WireTransaction> get() = nonVerifiedTransactionWithLocations.values.map { it.transaction }
}

private fun ServiceHub.signInitialTransaction(wtx: WireTransaction): SignedTransaction {
    val pubKey = this.myInfo.legalIdentitiesAndCerts.first().owningKey
    val signatureMetadata = SignatureMetadata(myInfo.platformVersion, Crypto.findSignatureScheme(pubKey).schemeNumberID)
    val signableData = SignableData(wtx.id, signatureMetadata)
    val sig = keyManagementService.sign(signableData, pubKey)
    return SignedTransaction(wtx, listOf(sig))
}
