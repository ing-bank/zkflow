package zinc.transaction

import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.serialization.bfl.FixedLengthSerializationScheme
import com.ing.zknotary.common.serialization.json.corda.PublicInputSerializer
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.fixtures.contract.TestContract
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.lazyMapped
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializationMagic
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.transactions.ComponentGroup
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.loggerFor
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import zinc.types.setupTimed
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("slow")
class TransactionVerificationTest {
    private val log = loggerFor<TransactionVerificationTest>()

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

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

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
            wtx = wtx,
            emptyList(), emptyList()
        )

        val publicInput = PublicInput(
            transactionId = wtx.id,
            inputHashes = emptyList(),
            referenceHashes = emptyList()
        )

        // Uncomment this to test short dev cycles with the real circuit, but not yet real setup/prove/verify functions
        val actual = zincZKService.run(witness, publicInput)
        println(actual)
        actual shouldBe Json.encodeToString(PublicInputSerializer, publicInput)

        // Uncomment this and setup above to test with the real setup/prove/verify functions
//        val proof = zincZKService.proveTimed(witness, log)
//        zincZKService.verifyTimed(proof, publicInput, log)
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
        schemeId: Int = FixedLengthSerializationScheme.SCHEME_ID,
        additionalSerializationProperties: Map<Any, Any> = emptyMap()
    ): WireTransaction {
        val magic: SerializationMagic = CustomSerializationSchemeUtils.getCustomSerializationMagicFromSchemeId(schemeId)
        val serializationContext = SerializationDefaults.P2P_CONTEXT.withPreferredSerializationVersion(magic)
            .withProperties(additionalSerializationProperties)

        return SerializationFactory.defaultFactory.withCurrentContext(serializationContext) {
            WireTransaction(
                createDummyComponentGroups(
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
                    null,
                    null,
                    references,
                    null
                ),
                PrivacySalt(),
                digestService
            )
        }
    }

    @Suppress("LongParameterList")
    private fun createDummyComponentGroups(
        inputs: List<StateRef>,
        outputs: List<TransactionState<TestContract.TestState>>,
        commands: List<Command<*>>,
        attachments: List<SecureHash>,
        notary: Party?,
        timeWindow: TimeWindow?,
        references: List<StateRef>,
        networkParametersHash: SecureHash?
    ): List<ComponentGroup> {
        val serialize = { value: Any, _: Int -> OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        val componentGroupMap: MutableList<ComponentGroup> = mutableListOf()
        if (inputs.isNotEmpty()) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.INPUTS_GROUP.ordinal, inputs.map { OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5)) }))
        if (references.isNotEmpty()) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.REFERENCES_GROUP.ordinal, references.map { OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5)) }))
        if (outputs.isNotEmpty()) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.OUTPUTS_GROUP.ordinal, outputs.map { OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5)) }))
        // Adding commandData only to the commands group. Signers are added in their own group.
        if (commands.isNotEmpty()) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.COMMANDS_GROUP.ordinal, commands.map { OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5)) }))
        if (attachments.isNotEmpty()) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal, attachments.map { OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5)) }))
        if (notary != null) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.NOTARY_GROUP.ordinal, listOf(OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5))).lazyMapped(serialize)))
        if (timeWindow != null) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal, listOf(OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5))).lazyMapped(serialize)))
        // Adding signers to their own group. This is required for command visibility purposes: a party receiving
        // a FilteredTransaction can now verify it sees all the commands it should sign.
        if (commands.isNotEmpty()) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.SIGNERS_GROUP.ordinal, commands.map { it.signers }.map { OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5)) }))
        if (networkParametersHash != null) componentGroupMap.add(ComponentGroup(ComponentGroupEnum.PARAMETERS_GROUP.ordinal, listOf(OpaqueBytes(byteArrayOf(1, 2, 3, 4, 5)))))
        return componentGroupMap
    }

    private fun <R> withCustomSerializationEnv(block: () -> R): R {
        return createTestSerializationEnv(javaClass.classLoader).asTestContextEnv { block() }
    }
}
