package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.ZKJsonSerializationFactoryService
import com.ing.zknotary.common.states.ZKReferenceStateRef
import com.ing.zknotary.common.states.ZKStateRef
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import java.util.function.Predicate
import kotlin.test.assertEquals
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize
import net.corda.core.transactions.NetworkParametersHash
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ZKMerkleTreeTest {
    private val alice = TestIdentity.fresh("alice")
    private val bob = TestIdentity.fresh("bob")

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    @Test
    fun `can recalculate zkid based on serialized zkltx`() {
        ledgerServices.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            val ltx = wtx.toLedgerTransaction(ledgerServices)
            val serializationFactoryService = ZKJsonSerializationFactoryService()

            val ptx = ZKProverTransaction(
                ltx,
                serializationFactoryService,
                BLAKE2s256DigestService
            )

            val json = ptx.serialize(serializationFactoryService.factory)
            // println(String(json.bytes))
            val deserializedPtx = json.deserialize(serializationFactoryService.factory)
            assertEquals(ptx, deserializedPtx)

            val json2 = ptx.serialize(serializationFactoryService.factory)
            assertEquals(json, json2)

            val vtx = ptx.buildVerifierTransaction(Predicate {
                it is ZKStateRef || it is ZKReferenceStateRef || it is TimeWindow || it == ptx.notary || it is NetworkParametersHash
            })
            val amqp = vtx.serialize()
            val deserializedVtx = amqp.deserialize()
            assertEquals(vtx, deserializedVtx)

            assertEquals(ptx.id, deserializedVtx.id)

            deserializedVtx.verify()
        }
    }
}
