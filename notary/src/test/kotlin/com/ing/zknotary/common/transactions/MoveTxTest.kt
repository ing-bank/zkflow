package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.dactyloscopy.Dactyloscopist
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZKNulls
import com.ing.zknotary.common.zkp.ZincZKTransactionService
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.PrivacySalt
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups
import net.corda.core.internal.unspecifiedCountry
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
@Disabled("Now that we need history for proof generation, this test will no longer work. Solution: use issuance tx for it")
@Tag("slow")
class MoveTxTest {
    private val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
    private lateinit var alice: TestIdentity
    private lateinit var bob: TestIdentity
    private lateinit var notary: Party
    private lateinit var ledgerServices: MockServices

    private val circuitFolder = File("${System.getProperty("user.dir")}/../prover/circuits/move").absolutePath
    private val artifactFolder = File("$circuitFolder/artifacts")
    private val zincTxZKService = ZincZKTransactionService(
        circuitFolder,
        artifactFolder = artifactFolder.absolutePath,
        buildTimeout = Duration.ofSeconds(10 * 60),
        setupTimeout = Duration.ofSeconds(10 * 60),
        provingTimeout = Duration.ofSeconds(10 * 60),
        verificationTimeout = Duration.ofSeconds(10 * 60)
    )

    init {
        artifactFolder.mkdirs()
        val setupDuration = measureTime {
            zincTxZKService.setup()
        }
        println("Setup duration: $setupDuration")
    }

    @AfterEach
    fun `remove zinc files`() {
        zincTxZKService.cleanup()
    }

    @BeforeEach
    fun setup() {
        alice = TestIdentity(
            CordaX500Name("alice", fixedKeyPair.public.toStringShort(), CordaX500Name.unspecifiedCountry), fixedKeyPair
        )

        bob = TestIdentity(
            CordaX500Name("bob", fixedKeyPair.public.toStringShort(), CordaX500Name.unspecifiedCountry), fixedKeyPair
        )

        notary = Party(
            CordaX500Name("notary", fixedKeyPair.public.toStringShort(), CordaX500Name.unspecifiedCountry),
            fixedKeyPair.public
        )

        ledgerServices = MockServices(
            listOf("com.ing.zknotary.common.contracts"),
            alice
        )
    }

    @Test
    fun `merkle roots computed in Corda and Zinc coincide`() {
        ledgerServices.ledger(notary) {
            val wtx = run {
                // This fixes the content of witness completely.
                val wtx = moveTestsState(createTestsState(owner = alice, value = 100), newOwner = bob)
                verifies()

                WireTransaction(
                    createComponentGroups(
                        inputs = wtx.inputs,
                        outputs = wtx.outputs,
                        commands = wtx.commands,
                        attachments = wtx.attachments,
                        notary = wtx.notary,
                        timeWindow = wtx.timeWindow,
                        references = wtx.references,
                        networkParametersHash = wtx.networkParametersHash
                    ),
                    privacySalt = PrivacySalt(ByteArray(wtx.privacySalt.size) { 1 })
                )
            }

            val ltx = wtx.toLedgerTransaction(ledgerServices)

            val ptx = ZKProverTransactionFactory.create(
                ltx,
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = PedersenDigestService
            )

            // TODO: replace this duplicated code from MoveBackChainTest
            val paddingNonce = ptx.componentGroupLeafDigestService.zeroHash
            val fillerOutput = ptx.componentPaddingConfiguration.filler(ComponentGroupEnum.OUTPUTS_GROUP)
                ?: error("Expected a filler object")
            require(fillerOutput is ComponentPaddingConfiguration.Filler.TransactionState) { "Expected filler of type TransactionState" }
            val paddingHash =
                ptx.componentGroupLeafDigestService.hash(paddingNonce.bytes + Dactyloscopist.identify(fillerOutput.content))

            val witness = Witness(
                ptx,
                inputNonces = ptx.padded.inputs().map { paddingNonce },
                referenceNonces = ptx.padded.references().map { paddingNonce }
            )

            val publicInput = PublicInput(
                ptx.id,
                inputHashes = ptx.padded.inputs().map { paddingHash },
                referenceHashes = ptx.padded.references().map { paddingHash }
            )

            var proof: ByteArray
            val proveDuration = measureTime {
                proof = zincTxZKService.prove(witness)
            }
            println("Prove duration: $proveDuration")

            val verifyDuration = measureTime {
                zincTxZKService.verify(proof, publicInput)
            }
            println("Verify duration: $verifyDuration")
        }
    }
}
