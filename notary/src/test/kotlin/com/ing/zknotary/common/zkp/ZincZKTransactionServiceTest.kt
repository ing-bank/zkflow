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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

@Disabled("Ignored until we have the witness and public input structure finalized")
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

    @BeforeEach
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

    @AfterEach
    fun `remove zinc files`() {
        zincTransactionZKService.cleanup()
    }

    @Test
    fun `valid witness verifies`() {
        ledgerServices.ledger {
            val proof = zincTransactionZKService.prove(
                Witness(
                    ptx,
                    inputNonces = ptx.padded.inputs().map { PedersenDigestService.zeroHash },
                    referenceNonces = ptx.padded.references().map { PedersenDigestService.zeroHash }
                )
            )
            val testList = listOf<SecureHash>(PedersenDigestService.allOnesHash)
            val correctPublicInput =
                PublicInput(SecureHash.Pedersen(ptx.privacySalt.bytes), testList, testList)

            zincTransactionZKService.verify(proof, correctPublicInput)
        }
    }

    @Test
    fun `verification fails on public data mismatch`() {
        ledgerServices.ledger {
            val proof = zincTransactionZKService.prove(
                Witness(
                    ptx,
                    inputNonces = ptx.padded.inputs().map { PedersenDigestService.zeroHash },
                    referenceNonces = ptx.padded.references().map { PedersenDigestService.zeroHash }
                )
            )
            val testList = listOf<SecureHash>(PedersenDigestService.allOnesHash)
            val wrongPublicData = PublicInput(PedersenDigestService.zeroHash, testList, testList)

            assertFailsWith(ZKVerificationException::class) {
                zincTransactionZKService.verify(proof, wrongPublicData)
            }
        }
    }
}
