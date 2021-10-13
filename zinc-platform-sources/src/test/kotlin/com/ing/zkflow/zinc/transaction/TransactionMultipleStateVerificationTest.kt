package com.ing.zknotary.zinc.transaction

import com.ing.zkflow.common.zkp.ZincZKTransactionService
import com.ing.zkflow.testing.dsl.VerificationMode
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.testing.fixtures.contract.TestMultipleStateContract
import com.ing.zkflow.testing.zkp.ZKNulls
import net.corda.core.utilities.loggerFor
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Tag("slow")
class TransactionMultipleStateVerificationTest {
    private val log = loggerFor<TransactionMultipleStateVerificationTest>()

    private val zincZKTransactionService: ZincZKTransactionService = ZincZKTransactionService(MockServices())

    private val notary = ZKNulls.NULL_PARTY

    private val cordapps = listOf(
        "com.ing.zkflow.testing.fixtures.contract"
    )

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
            val createState1 = TestMultipleStateContract.TestState1(alice, value = 88)
            val createState2 = TestMultipleStateContract.TestState2(alice, value = 99, list = listOf(42, 43))
            val createTx = zkTransaction {
                output(TestMultipleStateContract.PROGRAM_ID, createState1)
                output(TestMultipleStateContract.PROGRAM_ID, createState2)
                command(alice.owningKey, TestMultipleStateContract.Create())
                timeWindow(time = Instant.EPOCH)
                verifies(VerificationMode.RUN)
            }
            val utxo1 = createTx.outRef<TestMultipleStateContract.TestState1>(0)
            val utxo2 = createTx.outRef<TestMultipleStateContract.TestState2>(1)
            val moveState1 = TestMultipleStateContract.TestState1(bob, value = createState1.value)
            val moveState2 = TestMultipleStateContract.TestState2(bob, value = createState2.value, list = createState2.list)
            zkTransaction {
                input(utxo1.ref)
                input(utxo2.ref)
                output(TestMultipleStateContract.PROGRAM_ID, moveState1)
                output(TestMultipleStateContract.PROGRAM_ID, moveState2)
                timeWindow(time = Instant.EPOCH)
                command(listOf(alice.owningKey, bob.owningKey), TestMultipleStateContract.Move())
                verifies(VerificationMode.RUN)
            }
        }
    }
}
