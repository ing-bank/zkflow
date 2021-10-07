package com.ing.zkflow.zinc.transaction

import com.ing.zkflow.common.crypto.zinc
import com.ing.zkflow.common.serialization.bfl.BFLSerializationScheme
import com.ing.zkflow.common.zkp.ZincZKTransactionService
import com.ing.zkflow.testing.dsl.VerificationMode
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.testing.fixtures.contract.TestContract
import com.ing.zkflow.testing.zkp.ZKNulls
import com.ing.zkflow.zinc.types.setupTimed
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
import net.corda.core.serialization.SerializationDefaults
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SerializationMagic
import net.corda.core.serialization.internal.CustomSerializationSchemeUtils
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("slow")
class TransactionVerificationTest {
    private val log = loggerFor<TransactionVerificationTest>()
    private val runOnly = true

    private val zincZKTransactionService: ZincZKTransactionService = ZincZKTransactionService(MockServices())

    private val createZKService =
        zincZKTransactionService.zkServiceForTransactionMetadata(TestContract.Create().transactionMetadata)
    private val moveZKService = zincZKTransactionService.zkServiceForTransactionMetadata(TestContract.Move().transactionMetadata)

    private val notary = ZKNulls.NULL_PARTY

    private val cordapps = listOf(
        "com.ing.zkflow.testing.fixtures.contract"
    )

    init {
        if (!runOnly) {
            createZKService.setupTimed(log)
            moveZKService.setupTimed(log)
        }
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
    @ExperimentalTime
    @Test
    fun `dsl test`() {
        val alice = TestIdentity.fresh("Alice").party.anonymise()
        val bob = ZKNulls.NULL_ANONYMOUS_PARTY
        val services = MockServices(cordapps)
        // services.zkLedger(zkService = MockZKTransactionService(services)) {
        services.zkLedger {
            val createState = TestContract.TestState(alice, value = 88)
            val createTx = zkTransaction {
                output(TestContract.PROGRAM_ID, createState)
                command(alice.owningKey, TestContract.Create())
                timeWindow(time = Instant.EPOCH)
                verifies(VerificationMode.RUN)
            }
            val utxo = createTx.outRef<TestContract.TestState>(0)
            zkTransaction {
                val moveState = TestContract.TestState(bob, value = createState.value)
                input(utxo.ref)
                output(TestContract.PROGRAM_ID, moveState)
                timeWindow(time = Instant.EPOCH)
                command(listOf(alice.owningKey, bob.owningKey), TestContract.Move())
                verifies(VerificationMode.RUN)
            }
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
                    outputs.map { TransactionState(it.state, it.contract, notary) },
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
}
