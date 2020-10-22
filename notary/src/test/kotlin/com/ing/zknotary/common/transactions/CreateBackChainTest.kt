package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.testing.fixed
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.ByteBuffer
import kotlin.time.ExperimentalTime

@ExperimentalTime
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateBackChainTest {
    private val alice = TestIdentity.fixed("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fixed("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice,
        testNetworkParameters(minimumPlatformVersion = 6),
        bob
    )

    private lateinit var createWtx: WireTransaction
    private lateinit var transactionToVerify: WireTransaction

    // User real Zinc circuit, or mocked circuit (that checks same rules)
    private val mockZKP = false
    private val circuits = mapOf(
        TestContract.Create().id to "${System.getProperty("user.dir")}/../prover/circuits/create"
    ).map {
        SecureHash.Companion.sha256(ByteBuffer.allocate(4).putInt(it.key).array()) as SecureHash to it.value
    }.toMap()

    private val verificationService by lazy {
        val proverService = if (mockZKP) {
            ProverService(ledgerServices)
        } else {
            ProverService(ledgerServices, circuits)
        }

        proverService.prove(transactionToVerify)

        VerificationService(proverService)
    }

    @AfterAll
    fun `remove zinc files`() {
        verificationService.cleanup()
    }

    @BeforeAll
    fun setup() {
        ledgerServices.ledger {
            createWtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Alice's asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()

            println("CREATE \t\t\tWTX: ${createWtx.id.toString().take(8)}")
        }

        transactionToVerify = createWtx
    }

    @Test
    @Tag("slow")
    fun `We deterministically build, prove and verify graph of ZKVerifierTransactions based on graph of SignedTransactions`() {
        verificationService.verify(transactionToVerify)
    }
}
