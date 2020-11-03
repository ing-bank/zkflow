package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.testing.fixed
import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.common.zkp.ZincZKTransactionService
import com.ing.zknotary.node.services.collectVerifiedDependencies
import junit.framework.TestCase
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.loggerFor
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
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
    private val alice = TestIdentity.fixed("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fixed("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice,
        testNetworkParameters(minimumPlatformVersion = 6),
        bob
    )

    // User real Zinc circuit, or mocked circuit (that checks same rules)
    private val mockZKP = false

    // Mocked txs:
    private lateinit var createWtx: WireTransaction
    private lateinit var moveWtx: WireTransaction
    private lateinit var create2Wtx: WireTransaction
    private lateinit var move2Wtx: WireTransaction

    private val logger = loggerFor<BackChainTest>()

    init {
        logger.info("Mocking up some txs")
        ledgerServices.ledger {
            createWtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Alice's asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()
            logger.info("CREATE \tWTX: ${createWtx.id.toString().take(8)}")

            val createdState = createWtx.outRef<TestContract.TestState>(0)

            moveWtx = transaction {
                input(createdState.ref)
                output(TestContract.PROGRAM_ID, createdState.state.data.withNewOwner(bob.party).ownableState)
                command(listOf(createdState.state.data.owner.owningKey), TestContract.Move())
                verifies()
            }
            verifies()
            logger.info("MOVE \t\tWTX: ${moveWtx.id.toString().take(8)}")

            val movedState = moveWtx.outRef<TestContract.TestState>(0)

            create2Wtx = transaction {
                command(listOf(alice.publicKey), TestContract.Create())
                output(TestContract.PROGRAM_ID, "Bob's reference asset", TestContract.TestState(alice.party))
                verifies()
            }
            verifies()
            logger.info("CREATE2 \tWTX: ${create2Wtx.id.toString().take(8)}")

            move2Wtx = transaction {
                input(movedState.ref)
                output(TestContract.PROGRAM_ID, movedState.state.data.withNewOwner(alice.party).ownableState)
                command(listOf(movedState.state.data.owner.owningKey), TestContract.Move())
                reference("Bob's reference asset")
                verifies()
            }
            verifies()
            logger.info("ANOTHERMOVE \tWTX: ${move2Wtx.id.toString().take(8)}")
        }
    }

    @Nested
    inner class Create {
        private val proverService = if (mockZKP) {
            ProverService(ledgerServices)
        } else {
            val circuits = mapOf(
                TestContract.Create().id to "${System.getProperty("user.dir")}/../prover/circuits/create"
            ).map {
                SecureHash.Companion.sha256(
                    ByteBuffer.allocate(4).putInt(it.key).array()
                ) as SecureHash to setupCircuit(it.value)
            }.toMap()

            ProverService(ledgerServices, circuits)
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
            ProverService(ledgerServices)
        } else {
            val circuits = mapOf(
                TestContract.Create().id to "${System.getProperty("user.dir")}/../prover/circuits/create",
                TestContract.Move().id to "${System.getProperty("user.dir")}/../prover/circuits/move"
            ).map {
                SecureHash.Companion.sha256(
                    ByteBuffer.allocate(4).putInt(it.key).array()
                ) as SecureHash to setupCircuit(it.value)
            }.toMap()

            ProverService(ledgerServices, circuits)
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

    private fun setupCircuit(circuitFolder: String): ZKTransactionService {
        logger.info("Setting up circuit: $circuitFolder")

        val circuitFolder = File(circuitFolder).absolutePath
        val artifactFolder = File("$circuitFolder/artifacts")
        artifactFolder.mkdirs()

        val zincZKTransactionService = ZincZKTransactionService(
            circuitFolder,
            artifactFolder = artifactFolder.absolutePath,
            buildTimeout = Duration.ofSeconds(10 * 60),
            setupTimeout = Duration.ofSeconds(10 * 60),
            provingTimeout = Duration.ofSeconds(10 * 60),
            verificationTimeout = Duration.ofSeconds(10 * 60)
        )

        val setupDuration = measureTime {
            zincZKTransactionService.setup()
        }

        logger.info("Completed set up for circuit: $circuitFolder ")
        logger.debug("Duration: ${setupDuration.inMinutes} mins")

        return zincZKTransactionService
    }
}
