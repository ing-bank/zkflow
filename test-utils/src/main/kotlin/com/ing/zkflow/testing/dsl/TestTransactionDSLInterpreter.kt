import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.testing.dsl.DuplicateOutputLabel
import com.ing.zkflow.testing.dsl.EnforceVerifyOrFail
import com.ing.zkflow.testing.dsl.LedgerDSL
import com.ing.zkflow.testing.dsl.OutputStateLookup
import com.ing.zkflow.testing.dsl.TransactionDSL
import com.ing.zkflow.testing.dsl.TransactionDSLInterpreter
import com.ing.zkflow.testing.dsl.VerificationMode
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.cordapp.CordappProvider
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.AttachmentTrustCalculator
import net.corda.core.internal.ResolveTransactionsFlow
import net.corda.core.internal.ServiceHubCoreInternal
import net.corda.core.internal.TransactionsResolver
import net.corda.core.internal.notary.NotaryService
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.TransactionStorage
import net.corda.core.serialization.internal.AttachmentsClassLoaderCache
import net.corda.core.serialization.internal.AttachmentsClassLoaderCacheImpl
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.loggerFor
import net.corda.node.services.DbTransactionsResolver
import net.corda.node.services.attachments.NodeAttachmentTrustCalculator
import net.corda.node.services.persistence.AttachmentStorageInternal
import net.corda.testing.internal.MockCordappProvider
import net.corda.testing.internal.TestingNamedCacheFactory
import net.corda.testing.internal.services.InternalMockAttachmentStorage
import net.corda.testing.services.MockAttachmentStorage
import java.security.PublicKey
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.reflect.full.primaryConstructor

/**
 * This interpreter builds a transaction, and [TransactionDSL.verifies] that the resolved transaction is correct. Note
 * that transactions corresponding to input states are not verified. Use [LedgerDSL.verifies] for that.
 *
 * To build the transaction a `TransactionBuilder` is used.
 */
@Suppress("TooManyFunctions")
public data class TestTransactionDSLInterpreter private constructor(
    override val ledgerInterpreter: TestZKLedgerDSLInterpreter,
    val transactionBuilder: TransactionBuilder,
    internal val labelToIndexMap: HashMap<String, Int>
) : TransactionDSLInterpreter, OutputStateLookup by ledgerInterpreter {

    public constructor(
        ledgerInterpreter: TestZKLedgerDSLInterpreter,
        transactionBuilder: TransactionBuilder
    ) : this(ledgerInterpreter, transactionBuilder, HashMap())

    private val log = loggerFor<TestZKTransactionDSLInterpreter>()

    // Implementing [ServiceHubCoreInternal] allows better use in internal Corda tests
    val services: ServiceHubCoreInternal = object : ServiceHubCoreInternal, ServiceHub by ledgerInterpreter.services {

        // [validatedTransactions.getTransaction] needs overriding as there are no calls to
        // [ServiceHub.recordTransactions] in the test dsl
        override val validatedTransactions: TransactionStorage =
            object : TransactionStorage by ledgerInterpreter.services.validatedTransactions {
                override fun getTransaction(id: SecureHash): SignedTransaction? =
                    ledgerInterpreter.getTransaction(id)
            }

        override val externalOperationExecutor: ExecutorService = Executors.newFixedThreadPool(
            2,
            ThreadFactoryBuilder().setNameFormat("flow-external-operation-thread").build()
        )

        override val attachmentTrustCalculator: AttachmentTrustCalculator =
            ledgerInterpreter.services.attachments.let {
                // Wrapping to a [InternalMockAttachmentStorage] is needed to prevent leaking internal api
                // while still allowing the tests to work
                NodeAttachmentTrustCalculator(
                    attachmentStorage = if (it is MockAttachmentStorage) {
                        InternalMockAttachmentStorage(it)
                    } else {
                        it as AttachmentStorageInternal
                    },
                    cacheFactory = TestingNamedCacheFactory()
                )
            }

        override fun createTransactionsResolver(flow: ResolveTransactionsFlow): TransactionsResolver =
            DbTransactionsResolver(flow)

        override fun loadState(stateRef: StateRef) =
            ledgerInterpreter.resolveStateRef<ContractState>(stateRef)

        override fun loadStates(stateRefs: Set<StateRef>): Set<StateAndRef<ContractState>> {
            return stateRefs.map { StateAndRef(loadState(it), it) }.toSet()
        }

        override val cordappProvider: CordappProvider =
            ledgerInterpreter.services.cordappProvider

        override val notaryService: NotaryService? = null

        override val attachmentsClassLoaderCache: AttachmentsClassLoaderCache = AttachmentsClassLoaderCacheImpl(
            TestingNamedCacheFactory()
        )
    }

    // Hack to reuse the LedgerInterpreter's ZKTransactionService with the local ServiceHub, so transaction resolution will work.
    private val zkService: ZKTransactionService = ledgerInterpreter.zkService::class.primaryConstructor!!.call(services)

    private fun copy(): TestTransactionDSLInterpreter =
        TestTransactionDSLInterpreter(
            ledgerInterpreter = ledgerInterpreter,
            transactionBuilder = transactionBuilder.copy(),
            labelToIndexMap = HashMap(labelToIndexMap)
        )

    internal fun toWireTransaction() = transactionBuilder.toWireTransaction(services, ledgerInterpreter.serializationSchemeID)

    override fun input(stateRef: StateRef) {
        val state = ledgerInterpreter.resolveStateRef<ContractState>(stateRef)
        transactionBuilder.addInputState(StateAndRef(state, stateRef))
    }

    override fun reference(stateRef: StateRef) {
        val state = ledgerInterpreter.resolveStateRef<ContractState>(stateRef)
        @Suppress("DEPRECATION") // Will remove when feature finalised.
        transactionBuilder.addReferenceState(StateAndRef(state, stateRef).referenced())
    }

    override fun output(
        contractClassName: ContractClassName,
        label: String?,
        notary: Party,
        encumbrance: Int?,
        attachmentConstraint: AttachmentConstraint,
        contractState: ContractState
    ) {
        transactionBuilder.addOutputState(contractState, contractClassName, notary, encumbrance, attachmentConstraint)
        if (label != null) {
            if (label in labelToIndexMap) {
                throw DuplicateOutputLabel(label)
            } else {
                val outputIndex = transactionBuilder.outputStates().size - 1
                labelToIndexMap[label] = outputIndex
            }
        }
    }

    override fun attachment(attachmentId: SecureHash) {
        transactionBuilder.addAttachment(attachmentId)
    }

    override fun command(signers: List<PublicKey>, commandData: CommandData) {
        val command = Command(commandData, signers)
        transactionBuilder.addCommand(command)
    }

    override fun verifies(mode: VerificationMode): EnforceVerifyOrFail {
        // Verify on a copy of the transaction builder, so if it's then further modified it doesn't error due to
        // the existing signature
        val txb = transactionBuilder.copy()

        val wtx = txb.toWireTransaction(services)
        wtx.toLedgerTransaction(services).verify()

        return EnforceVerifyOrFail.Token
    }

    override fun timeWindow(data: TimeWindow) {
        transactionBuilder.setTimeWindow(data)
    }

    override fun _tweak(dsl: TransactionDSLInterpreter.() -> EnforceVerifyOrFail): EnforceVerifyOrFail = copy().dsl()

    override fun _attachment(contractClassName: ContractClassName) {
        attachment(
            (services.cordappProvider as MockCordappProvider).addMockCordapp(
                contractClassName,
                services.attachments as MockAttachmentStorage
            )
        )
    }

    override fun _attachment(contractClassName: ContractClassName, attachmentId: AttachmentId, signers: List<PublicKey>) {
        attachment(
            (services.cordappProvider as MockCordappProvider).addMockCordapp(
                contractClassName,
                services.attachments as MockAttachmentStorage,
                attachmentId,
                signers
            )
        )
    }

    override fun _attachment(
        contractClassName: ContractClassName,
        attachmentId: AttachmentId,
        signers: List<PublicKey>,
        jarManifestAttributes: Map<String, String>
    ) {
        attachment(
            (services.cordappProvider as MockCordappProvider).addMockCordapp(
                contractClassName,
                services.attachments as MockAttachmentStorage,
                attachmentId,
                signers,
                jarManifestAttributes
            )
        )
    }
}
