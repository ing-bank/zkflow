package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKProverTransactionFactory
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.PedersenDigestService
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Duration
import kotlin.test.assertFailsWith

class ZincZKTransactionServiceTest {
    private val circuitSourcePath: String = javaClass.getResource("/ZincZKTransactionService/src/main.zn").path
    private val zincZKService = ZincZKTransactionService(
        circuitSrcPath = circuitSourcePath,
        artifactFolder = File(circuitSourcePath).parent,
        buildTimeout = Duration.ofSeconds(5),
        setupTimeout = Duration.ofSeconds(30),
        provingTimeout = Duration.ofSeconds(30),
        verificationTimeout = Duration.ofSeconds(1)
    )
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fresh("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    private lateinit var ptx: ZKProverTransaction

    @Before
    fun setup() {
        ledgerServices.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            ptx = ZKProverTransactionFactory.create(
                wtx.toLedgerTransaction(ledgerServices),
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = PedersenDigestService
            )
        }
    }

    init {
        zincZKService.setup()
    }

    @Test
    fun `valid witness verifies`() {
        ledgerServices.ledger {
            val proof = zincZKService.prove(Witness(ptx))
            val correctPublicInput = PublicInput(ptx.id)

            zincZKService.verify(proof, correctPublicInput)
        }
    }

    @Test
    fun `verification fails on public data mismatch`() {
        ledgerServices.ledger {
            val proof = zincZKService.prove(Witness(ptx))
            val wrongPublicData = PublicInput(PedersenDigestService.zeroHash)

            assertFailsWith(ZKVerificationException::class) {
                zincZKService.verify(proof, wrongPublicData)
            }
        }
    }
}
