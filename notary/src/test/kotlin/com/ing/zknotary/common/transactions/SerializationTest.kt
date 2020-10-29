package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.ZincSerializationFactory
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.mockEmptyProof
import com.ing.zknotary.nodes.services.MockZKProverTransactionStorage
import com.ing.zknotary.notary.transactions.createTestWireTransaction
import junit.framework.TestCase.assertEquals
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.sign
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.createMockCordaService
import net.corda.testing.node.ledger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SerializationTest {
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fresh("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    private lateinit var ptx: ZKProverTransaction
    private lateinit var vtx: ZKVerifierTransaction
    private lateinit var sigAlice: ByteArray

    @BeforeEach
    fun setup() {
        ledgerServices.ledger {
            val wtx = createTestWireTransaction(owner = alice, value = 1)

            // Build a ZKProverTransaction
            ptx = wtx.toZKProverTransaction(
                services = ledgerServices,
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = PedersenDigestService,
                zkProverTransactionStorage = createMockCordaService(ledgerServices, ::MockZKProverTransactionStorage)
            )

            // Collect signatures
            sigAlice = alice.keyPair.private.sign(ptx.id.bytes).bytes

            // build filtered ZKVerifierTransaction
            vtx = ptx.toZKVerifierTransaction(mockEmptyProof)
        }
    }

    @Test
    fun `Serialize public input to Zinc`() {
        ledgerServices.ledger {
            // Serialize for transport to Zinc
            val testList = listOf<SecureHash>(PedersenDigestService.allOnesHash)
            val publicInput = PublicInput(PedersenDigestService.zeroHash, testList, testList)
            val json = publicInput.serialize(ZincSerializationFactory)
            println(String(json.bytes))
            // TODO: do checks on JSON to confirm it is acceptable for Zinc
        }
    }

    @Test
    fun `Serialize witness to Zinc`() {
        ledgerServices.ledger {
            // Serialize for transport to Zinc
            val witness = Witness(
                ptx,
                inputNonces = ptx.padded.inputs().map { PedersenDigestService.zeroHash },
                referenceNonces = ptx.padded.references().map { PedersenDigestService.zeroHash }
            )
            val json = witness.serialize(ZincSerializationFactory)
            println(String(json.bytes))
            // TODO: do checks on JSON to confirm it is acceptable for Zinc
        }
    }

    @Test
    fun `VerifierTransaction from ProverTransaction has same Merkle root`() {
        ledgerServices.ledger {
            assertEquals(ptx.id, vtx.id)
        }
    }

    @Test
    fun `ProverTransaction survives Corda AMQP serialization`() {
        ledgerServices.ledger {
            val ptxAmqp = ptx.serialize()
            val deserializedptx = ptxAmqp.deserialize()
            assertEquals(ptx, deserializedptx)
        }
    }

    @Test
    fun `VerifierTransaction survives Corda AMQP serialization`() {
        ledgerServices.ledger {
            val vtxAmqp = vtx.serialize()
            val deserializedVtx = vtxAmqp.deserialize()
            assertEquals(vtx, deserializedVtx)
        }
    }
}
