package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.crypto.PEDERSEN
import com.ing.zknotary.common.crypto.blake2s256
import com.ing.zknotary.common.crypto.pedersen
import com.ing.zknotary.common.serializer.ZincSerializationFactory
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zknotary.notary.transactions.createIssuanceWtx
import junit.framework.TestCase.assertEquals
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SecureHash.Companion.allOnesHashFor
import net.corda.core.crypto.SecureHash.Companion.zeroHashFor
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.testing.core.TestIdentity
import net.corda.testing.internal.withTestSerializationEnvIfNotSet
import net.corda.testing.node.MockServices
import net.corda.testing.node.createMockCordaService
import net.corda.testing.node.ledger
import org.junit.jupiter.api.Test

class SerializationTest {
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val notary = TestIdentity.fresh("notary", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    private val ledger = ledgerServices.ledger {}

    private val wtx = ledger.createIssuanceWtx(owner = alice, value = 1)

    // Build a ZKProverTransaction
    private val ptx = withTestSerializationEnvIfNotSet {
        wtx.toZKProverTransaction(
            services = ledgerServices,
            componentGroupLeafDigestService = DigestService.blake2s256,
            nodeDigestService = DigestService.pedersen,
            zkVerifierTransactionStorage = createMockCordaService(
                ledgerServices,
                ::InMemoryZKVerifierTransactionStorage
            )
        )
    }

    // build filtered ZKVerifierTransaction
    private val vtx = ptx.toZKVerifierTransaction(ByteArray(0))

    @Test
    fun `Serialize public input to Zinc`() {
        withTestSerializationEnvIfNotSet {
            // Serialize for transport to Zinc
            val testList = listOf<SecureHash>(allOnesHashFor(SecureHash.PEDERSEN))
            val publicInput = PublicInput(zeroHashFor(SecureHash.PEDERSEN), testList, testList)
            publicInput.serialize(ZincSerializationFactory)
            // TODO: do checks on JSON to confirm it is acceptable for Zinc
        }
    }

    @Test
    fun `Serialize witness to Zinc`() {
        withTestSerializationEnvIfNotSet {
            // Serialize for transport to Zinc
            val witness = Witness(
                ptx,
                inputNonces = ptx.padded.inputs().map { zeroHashFor(SecureHash.PEDERSEN) },
                referenceNonces = ptx.padded.references().map { zeroHashFor(SecureHash.PEDERSEN) }
            )
            witness.serialize(ZincSerializationFactory)
            // TODO: do checks on JSON to confirm it is acceptable for Zinc
        }
    }

    @Test
    fun `VerifierTransaction from ProverTransaction has same Merkle root`() {
        withTestSerializationEnvIfNotSet {
            assertEquals(ptx.id, vtx.id)
        }
    }

    @Test
    fun `ProverTransaction survives Corda AMQP serialization`() {
        withTestSerializationEnvIfNotSet {
            val ptxAmqp = ptx.serialize()
            val deserializedptx = ptxAmqp.deserialize()
            assertEquals(ptx, deserializedptx)
        }
    }

    @Test
    fun `VerifierTransaction survives Corda AMQP serialization`() {
        withTestSerializationEnvIfNotSet {
            val vtxAmqp = vtx.serialize()
            val deserializedVtx = vtxAmqp.deserialize()
            assertEquals(vtx, deserializedVtx)
        }
    }
}
