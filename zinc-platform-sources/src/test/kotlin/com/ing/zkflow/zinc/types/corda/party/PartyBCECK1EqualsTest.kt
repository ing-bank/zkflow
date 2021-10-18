package com.ing.zkflow.zinc.types.corda.party

import com.ing.zkflow.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test

class PartyBCECK1EqualsTest {
    private val zincZKService = getZincZKService<PartyBCECK1EqualsTest>()

    companion object {
        private val aliceIdentity = TestIdentity.fresh("Alice", Crypto.ECDSA_SECP256K1_SHA256)
        private val bobIdentity = TestIdentity.fresh("Bob", Crypto.ECDSA_SECP256K1_SHA256)
        val alice = aliceIdentity.party
        val bob = bobIdentity.party
    }

    @Test
    fun `alice equals alice`() {
        performEqualityTest(alice, alice, true)
    }

    @Test
    fun `alice does not equals bob`() {
        performEqualityTest(alice, bob, false)
    }

    private fun performEqualityTest(
        left: Party,
        right: Party,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(BCECSurrogate.ENCODED_SIZE)
            )
            put(
                "right",
                right.toJsonObject(BCECSurrogate.ENCODED_SIZE)
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
