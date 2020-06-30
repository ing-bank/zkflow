package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.serializer.ZincSerializationFactoryService
import com.ing.zknotary.common.zkp.ZKNulls
import com.ing.zknotary.notary.transactions.createTestsState
import com.ing.zknotary.notary.transactions.moveTestsState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.sign
import net.corda.core.serialization.serialize
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class ZKMerkleTreeTest {
    private val alice = TestIdentity.fresh("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fresh("bob", Crypto.EDDSA_ED25519_SHA512)

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
            val serializationFactoryService = ZincSerializationFactoryService()

            val componentPadding = ComponentPadding.Builder()
                .inputs(2)
                .outputs(2)
                .references(2)
                .signers(2)
                .stateFiller(TestContract.TestState(ZKNulls.NULL_ANONYMOUS_PARTY, 0))
                .build()

            val ptx = ZKProverTransactionFactory.create(
                ltx,
                serializationFactoryService,
                componentPadding = componentPadding,
                componentGroupLeafDigestService = BLAKE2s256DigestService,
                nodeDigestService = BLAKE2s256DigestService // Should become Pedersen hash when available
            )

            // Collect signatures
            val sigAlice = alice.keyPair.private.sign(ptx.id.bytes).bytes

            val json = ptx.serialize(serializationFactoryService.factory)
            println(String(json.bytes))

            val cwd = System.getProperty("user.dir")
            val circuitWd = Paths.get("$cwd/../prover/ZKMerkleTree").normalize().toString()
            File("$circuitWd/data/witness.json").writeText(String(json.bytes))
        }
    }
}
