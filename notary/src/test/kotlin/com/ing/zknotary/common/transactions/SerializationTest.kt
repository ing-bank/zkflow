package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.crypto.PEDERSEN
import com.ing.zknotary.common.serializer.ZincSerializationFactory
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.notary.transactions.createIssuanceWtx
import junit.framework.TestCase.assertEquals
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SecureHash.Companion.allOnesHashFor
import net.corda.core.crypto.SecureHash.Companion.zeroHashFor
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.testing.core.TestIdentity
import net.corda.testing.internal.withTestSerializationEnvIfNotSet
import net.corda.testing.node.MockServices
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

    // build filtered ZKVerifierTransaction
    private val vtx = ZKVerifierTransaction(wtx, ByteArray(0))

    @Test
    fun `Serialize public input to Zinc`() {
        withTestSerializationEnvIfNotSet {
            // Serialize for transport to Zinc
            val testList = listOf(allOnesHashFor(SecureHash.PEDERSEN))
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
                wtx,
                wtx.toLedgerTransaction(ledgerServices).inputs,
                wtx.toLedgerTransaction(ledgerServices).references,
                inputNonces = wtx.inputs.map { zeroHashFor(SecureHash.PEDERSEN) },
                referenceNonces = wtx.references.map { zeroHashFor(SecureHash.PEDERSEN) }
            )
            witness.serialize(ZincSerializationFactory)
            // TODO: do checks on JSON to confirm it is acceptable for Zinc
        }
    }

    @Test
    fun `VerifierTransaction from ProverTransaction has same Merkle root`() {
        withTestSerializationEnvIfNotSet {
            assertEquals(wtx.id, vtx.id)
        }
    }

    @Test
    fun `ProverTransaction survives Corda AMQP serialization`() {
        withTestSerializationEnvIfNotSet {
            val ptxAmqp = wtx.serialize()
            val deserializedptx = ptxAmqp.deserialize()
            assertEquals(wtx, deserializedptx)
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
