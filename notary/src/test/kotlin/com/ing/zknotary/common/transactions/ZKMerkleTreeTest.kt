package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.ZincSerializationFactoryService
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZKNulls
import com.ing.zknotary.common.zkp.ZincZKTransactionService
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.createComponentGroups
import net.corda.core.internal.unspecifiedCountry
import net.corda.core.serialization.serialize
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.time.Duration

class ZKMerkleTreeTest {
    private val fixedKeyPair = ZKNulls.fixedKeyPair(Crypto.EDDSA_ED25519_SHA512)
    private lateinit var alice: TestIdentity
    private lateinit var bob: TestIdentity
    private lateinit var notary: Party
    private lateinit var ledgerServices: MockServices

    private val circuitFolder = File("${System.getProperty("user.dir")}/../prover/ZKMerkleTree").absolutePath
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
        zincTxZKService.setup()
    }

    @After
    fun `remove zinc files`() {
        zincTxZKService.cleanup()
    }

    @Before
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

    @Ignore
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

            val witness = Witness(ptx)
            val publicInput = PublicInput(ptx.id)

            // // This is left for debugging purposes, should this be required.
            // val serializationFactoryService = ZincSerializationFactoryService()
            //
            // val id = publicInput.serialize(serializationFactoryService.factory)
            // println("Expected = \n${String(id.bytes)}")
            //
            // val json = witness.serialize(serializationFactoryService.factory)
            // File("$circuitFolder/data/witness.json").writeText(String(json.bytes))

            val proof = zincTxZKService.prove(witness)
            zincTxZKService.verify(proof, publicInput)
        }
    }
}
