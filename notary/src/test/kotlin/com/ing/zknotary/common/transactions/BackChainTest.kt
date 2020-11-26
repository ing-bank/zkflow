package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.zkp.CircuitMetaData
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.common.zkp.ZincZKTransactionService
import com.ing.zknotary.node.services.collectVerifiedDependencies
import com.ing.zknotary.notary.transactions.createIssuanceWtx
import com.ing.zknotary.notary.transactions.createMoveWtx
import com.ing.zknotary.testing.fixed
import com.ing.zknotary.testing.zkp.MockZKTransactionService
import com.ing.zknotary.testing.zkp.ProverService
import com.ing.zknotary.testing.zkp.VerificationService
import junit.framework.TestCase
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.createMockCordaService
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
class BackChainTest {
    private val logger = loggerFor<BackChainTest>()

    private val alice = TestIdentity.fixed("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fixed("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice,
        testNetworkParameters(minimumPlatformVersion = 6),
        bob
    )

    private val ledger = ledgerServices.ledger {}

    // User real Zinc circuit, or mocked circuit (that checks same rules)
    // private val mockZKP = true
    val mockZKP = System.getProperty("MockZKP") != null

    // Mocked txs:
    private val createWtx: WireTransaction
    private val moveWtx: WireTransaction
    private val move2Wtx: WireTransaction

    init {
        logger.info("Mocking ZKP circuit: $mockZKP")
        ledger.apply {
            createWtx = createIssuanceWtx(alice, 1, "Alice's asset #1")
            moveWtx = createMoveWtx("Alice's asset #1", bob)
            createIssuanceWtx(bob, 2, "Bob's reference asset #1")
            move2Wtx = ledger.createMoveWtx(
                moveWtx.outRef<TestContract.TestState>(0),
                alice,
                "Bob's reference asset #1"
            )
        }
    }

    @Nested
    inner class Create {
        private val proverService = if (mockZKP) {
            ProverService(ledgerServices, setupMockCircuits())
        } else {
            ProverService(ledgerServices, setupCircuits(TestContract.Create()))
        }
        private val verificationService = VerificationService(proverService)

        @Test
        @Tag("slow")
        fun `We deterministically build, prove and verify graph of ZKVerifierTransactions based on graph of SignedTransactions`() {
            proverService.prove(createWtx)
            verificationService.verify(createWtx)
        }
    }

    @Nested
    inner class Move {
        private val proverService = if (mockZKP) {
            ProverService(ledgerServices, setupMockCircuits())
        } else {
            ProverService(ledgerServices, setupCircuits(TestContract.Create(), TestContract.Move()))
        }
        private val verificationService = VerificationService(proverService)

        @Test
        @Tag("slow")
        fun `Prover can fetch the complete tx graph for input StateRefs`() {
            val sortedDependencies = ledgerServices.validatedTransactions
                .collectVerifiedDependencies(move2Wtx.inputs)

            // We expect that the sorted deps of the anotherMoveWtx input is createWtx, moveWtx.
            TestCase.assertEquals(listOf(createWtx.id, moveWtx.id), sortedDependencies)
        }

        @Test
        @Tag("slow")
        fun `We deterministically build, prove and verify graph of ZKVerifierTransactions based on graph of SignedTransactions`() {
            proverService.prove(moveWtx)
            verificationService.verify(moveWtx)
        }
    }

    private fun setupCircuits(vararg commands: ZKCommandData) =
        commands.map {
            SecureHash.Companion.sha256(
                ByteBuffer.allocate(4).putInt(it.id).array()
            ) as SecureHash to setupCircuit(it.circuit)
        }.toMap()

    private fun setupCircuit(circuit: CircuitMetaData): ZKTransactionService {
        logger.info("Setting up circuit: ${circuit.folder}")

        val circuitFolderAbsolute = circuit.folder.absolutePath
        val artifactFolder = File("$circuitFolderAbsolute/artifacts")
        artifactFolder.mkdirs()

        val zincZKTransactionService = ZincZKTransactionService(
            circuitFolderAbsolute,
            artifactFolder = artifactFolder.absolutePath,
            buildTimeout = Duration.ofSeconds(10 * 60),
            setupTimeout = Duration.ofSeconds(10 * 60),
            provingTimeout = Duration.ofSeconds(10 * 60),
            verificationTimeout = Duration.ofSeconds(10 * 60)
        )

        val setupDuration = measureTime {
            zincZKTransactionService.setup()
        }

        logger.info("Completed set up for circuit: $circuitFolderAbsolute ")
        logger.debug("Duration: ${setupDuration.inMinutes} mins")

        return zincZKTransactionService
    }

    private fun setupMockCircuits() = mapOf(
        SecureHash.allOnesHash as SecureHash to createMockCordaService(
            ledgerServices,
            ::MockZKTransactionService
        )
    )
}
