package com.ing.zknotary.zinc.types.party

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
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
