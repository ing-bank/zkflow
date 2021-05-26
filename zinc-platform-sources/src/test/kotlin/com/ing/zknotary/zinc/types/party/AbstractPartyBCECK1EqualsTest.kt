package com.ing.zknotary.zinc.types.party

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AbstractPartyBCECK1EqualsTest {
    private val zincZKService = getZincZKService<AbstractPartyBCECK1EqualsTest>()

    companion object {
        val alice = TestIdentity.fresh("Alice", Crypto.ECDSA_SECP256K1_SHA256).party
        val bob = TestIdentity.fresh("Bob", Crypto.ECDSA_SECP256K1_SHA256).party
    }

    @Test
    fun `alice equals alice`() {
        performEqualityTest(alice, alice, true)
    }

    @Test
    fun `alice equals anonymous alice`() {
        performEqualityTest(alice, alice.anonymise(), true)
    }

    @Test
    fun `anonymous alice equals anonymous alice`() {
        val anonymousAlice = alice.anonymise()
        performEqualityTest(anonymousAlice, anonymousAlice, true)
    }

    @Test
    fun `alice does not equals bob`() {
        performEqualityTest(alice, bob, false)
    }

    @Test
    fun `anonymous alice does not equals bob`() {
        performEqualityTest(alice.anonymise(), bob, false)
    }

    @Test
    fun `anonymous alice does not equals anonymous bob`() {
        performEqualityTest(alice.anonymise(), bob.anonymise(), false)
    }

    private fun performEqualityTest(
        left: AbstractParty,
        right: AbstractParty,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(
                    serialName = BCECSurrogate::class.findAnnotation<SerialName>()!!.value,
                    encodedSize = BCECSurrogate.ENCODED_SIZE
                )
            )
            put(
                "right",
                right.toJsonObject(
                    serialName = BCECSurrogate::class.findAnnotation<SerialName>()!!.value,
                    encodedSize = BCECSurrogate.ENCODED_SIZE
                )
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
