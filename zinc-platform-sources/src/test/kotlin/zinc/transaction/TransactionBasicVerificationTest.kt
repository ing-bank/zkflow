package zinc.transaction

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.fixtures.contract.DummyContract
import com.ing.zknotary.testing.fixtures.state.DummyState
import com.ing.zknotary.testing.serialization.getSerializationContext
import com.ing.zknotary.testing.serialization.serializeWithScheme
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import zinc.transaction.serializer.CommandDataSerializerMap
import zinc.transaction.serializer.ContractStateSerializerMap
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("slow")
class TransactionBasicVerificationTest {
    private val log = loggerFor<TransactionBasicVerificationTest>()

    private val circuitFolder: String = File("build/circuits/create").absolutePath

    private val zincZKService = ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(300),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

//    init {
//        zincZKService.setupTimed(log)
//    }

//    @AfterAll
//    fun `remove zinc files`() {
//        zincZKService.cleanup()
//    }

    /**
     * The witness, which is what we serialize for Zinc, contains the following items:
     *
     * 1. Already serialized & sized componentgroups, e.g. groups of bytearrays of the WireTransaction.
     * 2. Already serialized & sized TransactionState<T: ContractState> class instances for all UTXOs
     *    (outputs of the previous transaction) pointed to by the inputs and reference StateRefs
     *    serialized inside the inputs and references component groups of the WireTransaction.
     * 3. The nonces bytes for the UTXOs pointed to by the input and reference StateRefs.
     *    (Unsized because hashes are serialized and sized by nature? Or should this be serialized & sized also?)
     *
     * Then in Zinc, the following happens respectively:
     *
     * 1. Recalculate the Merkle root using the sized & serialized bytearrays of the ComponentGroup's as-is,
     * 2. ComponentGroup's are deserialized into the expected transaction structs used for contract rule validation.
     *    Rule violation fails proof generation.
     * 3. The Merkle root is 'compared' with the expected Merkle root from the public input
     *    which would fail proof verification if not matching.
     * 4. The sized & serialized UTXOs are hashed together with their nonces to get the Merkle tree hashes for the UTXOs.
     *    These are 'compared' with the UTXO hashes from the public input.
     *    This proves that the contract rules have been applied on inputs and references that are unchanged
     *    since they were created in the preceding transactions.
     * 5. The UTXOs are deserialized into the expected TransactionState<T> structs and used
     *    together with the transaction struct from 2. for contract rule verification.
     *
     * Please validate these assumptions:
     *
     * The only data type sent to Zinc via JSON are byte arrays?
     * On the Kotlin side, serialization and deserialization sizes and unsizes respectively, invisibly for the user.
     * On the Zinc side, we never serialize. On deserialization, unsizing does not happen.
     */
    @Test
    @Suppress("LongMethod")
    fun `Wire transaction serializes`() = withCustomSerializationEnv {
        ContractStateSerializerMap.register(DummyState::class, 1, DummyState.serializer())
        CommandDataSerializerMap.register(DummyContract.Relax::class, 2, DummyContract.Relax.serializer())
        CommandDataSerializerMap.register(DummyContract.Chill::class, 3, DummyContract.Chill.serializer())

        val state = DummyState.any()
        val alice = state.participants.first()
        val bob = TestIdentity.fresh("Bob").party

        val inputs = List(4) { dummyStateRef() }
        val constrainedOutputs = listOf(
            ConstrainedState(
                StateAndContract(state, DummyContract.PROGRAM_ID),
                HashAttachmentConstraint(SecureHash.zeroHash)
            )
        )
        val commands = listOf(Command(DummyContract.Chill(), listOf(alice.owningKey, bob.owningKey)))
        val attachments = List(4) { SecureHash.randomSHA256() }
        val notary = TestIdentity.fresh("Notary").party
        val timeWindow = TimeWindow.fromOnly(Instant.now())
        val references = List(2) { dummyStateRef() }
        val networkParametersHash = SecureHash.randomSHA256()

        // This functionality is duplicated from ZKTransaction.toWireTransaction()
        val command = commands.singleOrNull() ?: error("Single command per transaction is allowed")
        val zkCommand = command.value as? ZKCommandData ?: error("Command must implement ZKCommandData")
        val additionalSerializationProperties = mapOf<Any, Any>(TestBFLSerializationScheme.CONTEXT_KEY_CIRCUIT to zkCommand.circuit)

        val wtx = createWtx(
            inputs,
            constrainedOutputs,
            commands,
            attachments,
            notary,
            timeWindow,
            references,
            networkParametersHash,
            additionalSerializationProperties = additionalSerializationProperties
        ).serialize().deserialize() // Deserialization must be forced, otherwise lazily mapped values will be picked up.

        /*
         * Confirm that the contents are actually serialized with BFL and not with something else.
         * This assertion becomes important once we start using the real ZKTransactionBuilder
         */
        val bflSerializedFirstInput = inputs.first().serializeWithScheme(TestBFLSerializationScheme.SCHEME_ID)
        wtx.componentGroups[ComponentGroupEnum.INPUTS_GROUP.ordinal].components.first().copyBytes() shouldBe bflSerializedFirstInput.bytes

        wtx.outputs.zip(constrainedOutputs).forEach { (actual, expected) ->
            actual.data shouldBe expected.stateAndContract.state
            actual.contract shouldBe expected.stateAndContract.contract
            actual.notary shouldBe notary
            actual.constraint shouldBe expected.constraint
        }

        wtx.inputs shouldBe inputs
        wtx.commands shouldBe commands
        wtx.attachments shouldBe attachments
        wtx.notary shouldBe notary
        wtx.timeWindow shouldBe timeWindow
        wtx.references shouldBe references
        wtx.networkParametersHash shouldBe networkParametersHash
    }

    @Suppress("LongParameterList")
    private fun createWtx(
        inputs: List<StateRef> = emptyList(),
        outputs: List<ConstrainedState> = emptyList(),
        commands: List<Command<*>> = emptyList(),
        attachments: List<SecureHash> = emptyList(),
        notary: Party,
        timeWindow: TimeWindow = TimeWindow.fromOnly(Instant.now()),
        references: List<StateRef> = emptyList(),
        networkParametersHash: SecureHash = SecureHash.zeroHash,

        digestService: DigestService = DigestService.zinc,

        // The Id of the custom serializationScheme to use
        schemeId: Int = TestBFLSerializationScheme.SCHEME_ID,
        additionalSerializationProperties: Map<Any, Any> = emptyMap()
    ): WireTransaction {
        val serializationContext = getSerializationContext(schemeId, additionalSerializationProperties)

        return SerializationFactory.defaultFactory.withCurrentContext(serializationContext) {
            WireTransaction(
                createComponentGroups(
                    inputs,
                    outputs.map {
                        TransactionState(
                            data = it.stateAndContract.state,
                            it.stateAndContract.contract,
                            notary = notary,
                            constraint = it.constraint
                        )
                    },
                    commands,
                    attachments,
                    notary,
                    timeWindow,
                    references,
                    networkParametersHash
                ),
                PrivacySalt(),
                digestService
            )
        }
    }

    private fun <R> withCustomSerializationEnv(block: () -> R): R {
        return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv { block() }
    }

    private data class ConstrainedState(val stateAndContract: StateAndContract, val constraint: AttachmentConstraint)

    private fun dummyStateRef() = StateRef(SecureHash.randomSHA256(), Random.nextInt())
}