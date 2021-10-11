package com.ing.zkflow.zinc.types.corda.securehash

import com.ing.zkflow.crypto.ZINC
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class SecureHashEqualsTest {
    private val zincZKService = getZincZKService<SecureHashEqualsTest>()

    @Test
    fun `identity test`() {
        val identity = SecureHash.hashAs(SecureHash.SHA2_256, "identity".toByteArray())
        performEqualityTest(identity, identity, true)
    }

    @Test
    fun `different PrivacySalts should not be equal`() {
        val first = SecureHash.hashAs(SecureHash.SHA2_256, "first".toByteArray())
        val second = SecureHash.hashAs(SecureHash.SHA2_256, "second".toByteArray())
        performEqualityTest(first, second, false)
    }

    @Test
    fun `different PrivacySalts with different algorithms should not be equal`() {
        val first = SecureHash.hashAs(SecureHash.SHA2_256, "first".toByteArray())
        val second = SecureHash.hashAs(SecureHash.ZINC, "second".toByteArray())
        performEqualityTest(first, second, false)
    }

    private fun performEqualityTest(
        left: SecureHash,
        right: SecureHash,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
