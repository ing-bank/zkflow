package com.ing.zkflow.zinc.types.corda.party

import com.ing.zkflow.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test

class AnonymousPartyBCECK1EqualsTest {
    private val zincZKService = getZincZKService<AnonymousPartyBCECK1EqualsTest>()

    companion object {
        val alice = TestIdentity.fresh("Alice", Crypto.ECDSA_SECP256K1_SHA256).party.anonymise()
        val bob = TestIdentity.fresh("Bob", Crypto.ECDSA_SECP256K1_SHA256).party.anonymise()
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
        left: AnonymousParty,
        right: AnonymousParty,
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
