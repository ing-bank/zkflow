package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.testing.fixed
import com.ing.zknotary.node.services.collectVerifiedDependencies
import junit.framework.TestCase.assertEquals
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MoveBackChainTest {
    private val alice = TestIdentity.fixed("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fixed("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice,
        testNetworkParameters(minimumPlatformVersion = 6),
        bob
    )

    private lateinit var createWtx: WireTransaction
    private lateinit var moveWtx: WireTransaction
    private lateinit var anotherMoveWtx: WireTransaction
    // Which transaction to start verifying. This can be any transaction in the vault.
    private lateinit var transactionToVerify: WireTransaction

    // User real Zinc circuit, or mocked circuit (that checks same rules)
    private val mockZKP = true
    private val circuits = mapOf(
        TestContract.Create().id to "${System.getProperty("user.dir")}/../prover/circuits/create",
        TestContract.Move().id to "${System.getProperty("user.dir")}/../prover/circuits/move"
    ).map {
        SecureHash.Companion.sha256(ByteBuffer.allocate(4).putInt(it.key).array()) as SecureHash to it.value
    }.toMap()

    private val verificationService = if (mockZKP) {
        VerificationService.mocked(ledgerServices)
    } else {
        VerificationService(ledgerServices, circuits)
    }

    @AfterEach
    fun `remove zinc files`() {
        verificationService.cleanup()
    }

    @BeforeEach
    fun setup() {
        ledgerServices.ledger {
            createWtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Alice's asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()

            println("CREATE \t\t\tWTX: ${createWtx.id.toString().take(8)}")
            val createdState = createWtx.outRef<TestContract.TestState>(0)

            moveWtx = transaction {
                input(createdState.ref)
                output(TestContract.PROGRAM_ID, createdState.state.data.withNewOwner(bob.party).ownableState)
                command(listOf(createdState.state.data.owner.owningKey), TestContract.Move())
                verifies()
            }
            println("MOVE \t\t\tWTX: ${moveWtx.id.toString().take(8)}")

            val movedState = moveWtx.outRef<TestContract.TestState>(0)

            val create2Wtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Bob's reference asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()
            println("CREATE2 \t\tWTX: ${create2Wtx.id.toString().take(8)}")

            anotherMoveWtx = transaction {
                input(movedState.ref)
                output(TestContract.PROGRAM_ID, movedState.state.data.withNewOwner(alice.party).ownableState)
                command(listOf(movedState.state.data.owner.owningKey), TestContract.Move())
                reference("Bob's reference asset")
                verifies()
            }
            println("ANOTHERMOVE \tWTX: ${anotherMoveWtx.id.toString().take(8)}")

            verifies()
        }

        transactionToVerify = moveWtx
    }

    @Test
    @Tag("slow")
    fun `Prover can fetch the complete tx graph for input StateRefs`() {
        val sortedDependencies = ledgerServices.validatedTransactions
            .collectVerifiedDependencies(anotherMoveWtx.inputs)

        // We expect that the sorted deps of the anotherMoveWtx input is createWtx, moveWtx.
        assertEquals(listOf(createWtx.id, moveWtx.id), sortedDependencies)
    }

    @Test
    @Tag("slow")
    fun `We deterministically build, prove and verify graph of ZKVerifierTransactions based on graph of SignedTransactions`() {
        verificationService.verify(transactionToVerify)
    }
}
