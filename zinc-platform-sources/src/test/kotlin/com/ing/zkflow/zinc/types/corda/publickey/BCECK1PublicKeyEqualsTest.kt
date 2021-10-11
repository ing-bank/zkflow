package com.ing.zkflow.zinc.types.corda.publickey

import com.ing.zkflow.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.generateDifferentValueThan
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import org.junit.jupiter.api.Test
import java.security.PublicKey
import kotlin.time.ExperimentalTime

@ExperimentalTime
class BCECK1PublicKeyEqualsTest {
    private val zincZKService = getZincZKService<BCECK1PublicKeyEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(key, key, true)
    }

    @Test
    fun `different keys should not be equal`() {
        performEqualityTest(key, anotherKey, false)
    }

    private fun performEqualityTest(
        left: PublicKey,
        right: PublicKey,
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

    companion object {
        private val scheme = Crypto.ECDSA_SECP256K1_SHA256
        val key: PublicKey = Crypto.generateKeyPair(scheme).public
        val anotherKey: PublicKey = generateDifferentValueThan(key) {
            Crypto.generateKeyPair(scheme).public
        }
    }
}
