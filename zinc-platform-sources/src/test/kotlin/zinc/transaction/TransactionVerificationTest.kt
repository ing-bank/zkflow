package zinc.transaction

import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.testing.fixtures.contract.TestContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.CustomSerializationScheme
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializationMagic
import net.corda.core.serialization.SerializationSchemeContext
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ByteSequence
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("nightly")
class TransactionVerificationTest {
//    private val log = loggerFor<TransactionVerificationTest>()
//
//    private val circuitFolder: String = File("build/circuits/create").absolutePath

//    private val zincZKService = ZincZKService(
//        circuitFolder,
//        artifactFolder = circuitFolder,
//        buildTimeout = Duration.ofSeconds(5),
//        setupTimeout = Duration.ofSeconds(300),
//        provingTimeout = Duration.ofSeconds(300),
//        verificationTimeout = Duration.ofSeconds(1)
//    )

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
    fun `zinc verifies full create transaction`() = withCustomSerializationEnv {
        val wtx = createWtx(
            outputs = listOf(StateAndContract(TestContract.TestState(alice), TestContract.PROGRAM_ID)),
            commands = listOf(Command(TestContract.Create(), alice.owningKey))
        )

        val witness = Witness.fromWireTransaction(
            tx = wtx,
            inputStates = emptyList(),
            inputNonces = emptyList(),
            referenceStates = emptyList(),
            referenceNonces = emptyList()
        )

        val publicData = PublicInput(
            transactionId = wtx.id,
            inputHashes = emptyList(),
            referenceHashes = emptyList()
        )

        // TODO: Implement ZincSerializationFactory and refactor circuit to handle new witness format
        // val witnessJson = ZincSerializationFactory.serialize(witness)
        // val publicDataJson = ZincSerializationFactory.serialize(publicData)

        // val proof = zincZKService.proveTimed(witnessJson.toByteArray(), log)
        // zincZKService.verify(proof, publicDataJson.toByteArray())
    }

    class TestScheme : CustomSerializationScheme {

        companion object {
            const val SCHEME_ID = 777
        }

        override fun getSchemeId(): Int {
            return SCHEME_ID
        }

        override fun <T : Any> deserialize(
            bytes: ByteSequence,
            clazz: Class<T>,
            context: SerializationSchemeContext
        ): T {
            // TODO: use custom serializer here
            return SerializationFactory.defaultFactory.deserialize(bytes, clazz, SerializationDefaults.P2P_CONTEXT)
        }

        override fun <T : Any> serialize(obj: T, context: SerializationSchemeContext): ByteSequence {
            // TODO: use custom serializer here
            return obj.serialize(SerializationFactory.defaultFactory, SerializationDefaults.P2P_CONTEXT)
        }
    }

    @Suppress("LongParameterList")
    private fun createWtx(
        inputs: List<StateRef> = emptyList(),
        outputs: List<StateAndContract> = emptyList(),
        commands: List<Command<*>> = emptyList(),
        attachments: List<SecureHash> = emptyList(),
        notary: Party = this.notary,
        timeWindow: TimeWindow = TimeWindow.fromOnly(Instant.now()),
        references: List<StateRef> = emptyList(),
        networkParametersHash: SecureHash = SecureHash.zeroHash,

        digestService: DigestService = DigestService.zinc,

        // The Id of the custom serializationScheme to use
        schemeId: Int = TestScheme.SCHEME_ID,
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
                            TestContract.TestState(alice),
                            TestContract.PROGRAM_ID,
                            notary
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
}
