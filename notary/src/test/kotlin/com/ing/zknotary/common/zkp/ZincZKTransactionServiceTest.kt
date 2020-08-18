package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKProverTransactionFactory
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.SecureHash
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class ZincZKTransactionServiceTest {
    private val circuitFolder = javaClass.getResource("/ZincZKTransactionService").path
    private val zincTransactionZKService = ZincZKTransactionService(
        circuitFolder,
        artifactFolder = circuitFolder,
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
        zincTransactionZKService.setup()
    }

    @After
    fun `remove zinc files`() {
        zincTransactionZKService.cleanup()
    }

    @Test
    fun `valid witness verifies`() {
        ledgerServices.ledger {
            val proof = zincTransactionZKService.prove(Witness(ptx))
            val correctPublicInput = PublicInput(SecureHash.Pedersen(ptx.privacySalt.bytes))

            zincTransactionZKService.verify(proof, correctPublicInput)
        }
    }

    @Test
    fun `verification fails on public data mismatch`() {
        ledgerServices.ledger {
            val proof = zincTransactionZKService.prove(Witness(ptx))
            val wrongPublicData = PublicInput(PedersenDigestService.zeroHash)

            assertFailsWith(ZKVerificationException::class) {
                zincTransactionZKService.verify(proof, wrongPublicData)
            }
        }
    }
}
