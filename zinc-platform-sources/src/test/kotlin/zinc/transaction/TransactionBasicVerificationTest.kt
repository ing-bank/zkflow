package zinc.transaction

import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.fixtures.contract.DummyContract
import com.ing.zknotary.testing.fixtures.state.DummyState
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
import net.corda.core.internal.deserialiseCommands
import net.corda.core.internal.deserialiseComponentGroup
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializationMagic
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import zinc.transaction.envelope.Envelope
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlin.time.ExperimentalTime

@ExperimentalTime
// @Tag("slow")
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

    private val notary = TestIdentity.fresh("Notary").party
    private val alice = TestIdentity.fresh("Alice").party

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
     * * Already serialized & sized componentgroups, e.g. groups of bytearrays of the WireTransaction.
     * * Already serialized & sized TransactionState<T: ContractState> class instances for all UTXOs (outputs of the previous transaction) pointed to by the inputs and reference StateRefs serialized inside the inputs and references component groups of the WireTransaction.
     * * The nonces bytes for the UTXOs pointed to by the input and reference StateRefs. (Unsized because hashes are serialized and sized by nature? Or should this be serialized & sized also?)
     *
     * Then in Zinc, the following happens respectively:
     *
     * We recalculate the Merkle root using the sized & serialized bytearrays of the componentgroups as is.
     * Next, they are deserialized into the expected transaction structs used for contract rule validation. Rule violation fails proof generation.
     * Finally the Merkle root is 'compared' with the expected Merkle root from the public input, which would fail proof verification if not matching.
     * and 3. The sized & serialized UTXOs are hashed together with their nonces to get the Merkle tree hashes for the UTXOs. These are 'compared' with the UTXO hashes from the public input. This proves that the contract rules have been applied on inputs and references that are unchanged since they were created in the preceding transactions. Next, the UTXOs are deserialized into the expected TransactionState<T> structs and used, together with the transaction struct from 1. for contract rule verification.
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
        Envelope.register(DummyState::class, 1, DummyState.serializer())
        Envelope.register(DummyContract.Relax::class, 1, DummyContract.Relax.serializer())

        val state2 = DummyState.any()
        val alice = state2.participants.first()

        // Access elements to trigger deserialization in order defined by ComponentGroupEnum
        //     INPUTS_GROUP, // ordinal = 0.
        //     OUTPUTS_GROUP, // ordinal = 1.
        //     COMMANDS_GROUP, // ordinal = 2.
        //     ATTACHMENTS_GROUP, // ordinal = 3.
        //     NOTARY_GROUP, // ordinal = 4.
        //     TIMEWINDOW_GROUP, // ordinal = 5.
        //     SIGNERS_GROUP, // ordinal = 6.
        //     REFERENCES_GROUP, // ordinal = 7.
        //     PARAMETERS_GROUP

        val inputs = List(4) { dummyStateRef() }
        val constrainedOutputs = listOf(
            ConstrainedState(
                StateAndContract(state2, DummyContract.PROGRAM_ID),
                HashAttachmentConstraint(SecureHash.zeroHash)
            )
        )
        val commands = listOf(Command(DummyContract.Relax(), alice.owningKey))
        val attachments = List(4) { SecureHash.randomSHA256() }
        // notary is this.notary
        val timeWindow = TimeWindow.fromOnly(Instant.now())
        val references = List(2) { dummyStateRef() }
        val networkParametersHash = SecureHash.randomSHA256()

        val wtx = createWtx(
            inputs,
            constrainedOutputs,
            commands,
            attachments,
            notary,
            timeWindow,
            references,
            networkParametersHash
        )

        // Deserialization must be forced, otherwise lazily mapped values will be picked up.

        println("INPUTS")
        val actualInputs = deserialiseComponentGroup(
            wtx.componentGroups,
            StateRef::class,
            ComponentGroupEnum.INPUTS_GROUP,
            forceDeserialize = true
        )
        actualInputs.zip(inputs).forEach { (actual, expected) ->
            actual shouldBe expected
        }

        println("OUTPUTS")
        val actualOutputs = deserialiseComponentGroup(
            wtx.componentGroups,
            TransactionState::class,
            ComponentGroupEnum.OUTPUTS_GROUP,
            forceDeserialize = true
        )
        actualOutputs.zip(constrainedOutputs).forEach { (actual, expected) ->
            actual.data shouldBe expected.stateAndContract.state
            actual.contract shouldBe expected.stateAndContract.contract
            actual.notary shouldBe notary
            actual.constraint shouldBe expected.constraint
        }

        println("COMMANDS")
        val actualCommands = deserialiseCommands(
            wtx.componentGroups,
            digestService = DigestService.zinc,
            forceDeserialize = true
        )
        actualCommands.zip(commands).forEach { (actual, expected) ->
            actual shouldBe expected
            actual.signers shouldBe expected.signers
        }

        println("ATTACHMENTS")
        // Expected attachments are set in createWtx.
        val actualAttachments = deserialiseComponentGroup(
            wtx.componentGroups,
            SecureHash::class,
            ComponentGroupEnum.ATTACHMENTS_GROUP,
            forceDeserialize = true
        )
        actualAttachments.zip(attachments).forEach { (actual, expected) ->
            actual shouldBe expected
        }

        println("NOTARY")
        val actualNotary = deserialiseComponentGroup(
            wtx.componentGroups,
            Party::class,
            ComponentGroupEnum.NOTARY_GROUP,
            forceDeserialize = true
        ).single()
        actualNotary shouldBe notary

        println("TIMEWINDOW")
        val actualTimeWindow = deserialiseComponentGroup(
            wtx.componentGroups,
            TimeWindow::class,
            ComponentGroupEnum.TIMEWINDOW_GROUP,
            forceDeserialize = true
        ).single()
        actualTimeWindow shouldBe timeWindow

        println("REFERENCES")
        val actualReferences = deserialiseComponentGroup(
            wtx.componentGroups,
            StateRef::class,
            ComponentGroupEnum.REFERENCES_GROUP,
            forceDeserialize = true
        )
        actualReferences.zip(references).forEach { (actual, expected) ->
            actual shouldBe expected
        }

        val actualNetworkParametersHash = deserialiseComponentGroup(
            wtx.componentGroups,
            SecureHash::class,
            ComponentGroupEnum.PARAMETERS_GROUP,
            forceDeserialize = true
        ).single()
        actualNetworkParametersHash shouldBe networkParametersHash
    }

    @Suppress("LongParameterList")
    private fun createWtx(
        inputs: List<StateRef> = emptyList(),
        outputs: List<ConstrainedState> = emptyList(),
        commands: List<Command<*>> = emptyList(),
        attachments: List<SecureHash> = emptyList(),
        notary: Party = this.notary,
        timeWindow: TimeWindow = TimeWindow.fromOnly(Instant.now()),
        references: List<StateRef> = emptyList(),
        networkParametersHash: SecureHash = SecureHash.zeroHash,

        digestService: DigestService = DigestService.zinc,

        // The Id of the custom serializationScheme to use
        schemeId: Int = TestBFLSerializationScheme.SCHEME_ID,
        additionalSerializationProperties: Map<Any, Any> = emptyMap()
    ): WireTransaction {
        val magic: SerializationMagic = CustomSerializationSchemeUtils.getCustomSerializationMagicFromSchemeId(schemeId)
        val serializationContext = SerializationDefaults.P2P_CONTEXT.withPreferredSerializationVersion(magic)
            .withProperties(additionalSerializationProperties)

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
