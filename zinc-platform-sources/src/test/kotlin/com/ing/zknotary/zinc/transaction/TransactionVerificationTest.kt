package com.ing.zknotary.zinc.transaction

import com.ing.zknotary.common.crypto.zinc
import com.ing.zknotary.common.serialization.bfl.BFLSerializationScheme
import com.ing.zknotary.common.serialization.bfl.CommandDataSerializerMap
import com.ing.zknotary.common.serialization.bfl.ContractStateSerializerMap
import com.ing.zknotary.common.transactions.UtxoInfo
import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.fixtures.contract.TestContract
import com.ing.zknotary.zinc.types.proveTimed
import com.ing.zknotary.zinc.types.setupTimed
import com.ing.zknotary.zinc.types.verifyTimed
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
import net.corda.core.internal.createComponentGroups
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializationMagic
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import net.corda.coretesting.internal.asTestContextEnv
import net.corda.coretesting.internal.createTestSerializationEnv
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("slow")
class TransactionVerificationTest {
    private val log = loggerFor<TransactionVerificationTest>()

    private val createCircuitFolder: String = File("build/circuits/create").absolutePath
    private val moveCircuitFolder: String = File("build/circuits/move").absolutePath

    private val createZKService = ZincZKService(
        createCircuitFolder,
        artifactFolder = createCircuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(1000),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val moveZKService = ZincZKService(
        moveCircuitFolder,
        artifactFolder = moveCircuitFolder,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(1000),
        provingTimeout = Duration.ofSeconds(300),
        verificationTimeout = Duration.ofSeconds(1)
    )

    private val notary = TestIdentity.fresh("Notary").party
    private val alice = TestIdentity.fresh("Alice").party
    private val bob = TestIdentity.fresh("Blob").party

    init {
        createZKService.setupTimed(log)
        moveZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        createZKService.cleanup()
        moveZKService.cleanup()
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
        ContractStateSerializerMap.register(TestContract.TestState::class, 1, TestContract.TestState.serializer())
        CommandDataSerializerMap.register(TestContract.Create::class, 2, TestContract.Create.serializer())
        CommandDataSerializerMap.register(TestContract.Move::class, 3, TestContract.Move.serializer())

        val additionalSerializationProperties =
            mapOf<Any, Any>(BFLSerializationScheme.CONTEXT_KEY_CIRCUIT to TestContract.Create().circuit)

        // Create TX

        val createWtx = createWtx(
            outputs = listOf(StateAndContract(TestContract.TestState(alice.anonymise()), TestContract.PROGRAM_ID)),
            commands = listOf(Command(TestContract.Create(), alice.owningKey)),
            additionalSerializationProperties = additionalSerializationProperties
        )

        val witness = Witness.fromWireTransaction(
            wtx = createWtx,
            emptyList(), emptyList()
        )

        val publicInput = PublicInput(
            transactionId = createWtx.id,
            inputHashes = emptyList(),
            referenceHashes = emptyList()
        )

        createZKService.run(witness, null)

        val proof = createZKService.proveTimed(witness, log)
        createZKService.verifyTimed(proof, publicInput, log)

        // Move TX

        val additionalSerializationPropertiesForMove =
            mapOf<Any, Any>(BFLSerializationScheme.CONTEXT_KEY_CIRCUIT to TestContract.Move().circuit)

        val utxo = createWtx.outRef<TestContract.TestState>(0)
        val serializedUtxo = createWtx.componentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }.components[0]
        val nonce = createWtx.buildFilteredTransaction { true }
            .filteredComponentGroups.single { it.groupIndex == ComponentGroupEnum.OUTPUTS_GROUP.ordinal }
            .nonces[0]
        val inputHash = ZKVerifierTransaction.fromWireTransaction(createWtx, proof).outputHashes.single()

        val moveWtx = createWtx(
            inputs = listOf(utxo.ref),
            outputs = listOf(StateAndContract(TestContract.TestState(bob.anonymise()), TestContract.PROGRAM_ID)),
            commands = listOf(Command(TestContract.Move(), listOf(alice.owningKey, bob.owningKey))),
            additionalSerializationProperties = additionalSerializationPropertiesForMove
        )

        val moveWitness = Witness.fromWireTransaction(
            wtx = moveWtx,
            inputUtxoInfos = listOf(UtxoInfo(utxo.ref, serializedUtxo.bytes, nonce)),
            referenceUtxoInfos = emptyList()
        )

        val movePublicInput = PublicInput(
            transactionId = moveWtx.id,
            inputHashes = listOf(inputHash),
            referenceHashes = emptyList()
        )

        val moveProof = moveZKService.proveTimed(moveWitness, log)

        moveZKService.run(moveWitness, movePublicInput)

        moveZKService.verifyTimed(moveProof, movePublicInput, log)
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
        schemeId: Int = BFLSerializationScheme.SCHEME_ID,
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
                            TestContract.TestState(alice.anonymise()),
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
