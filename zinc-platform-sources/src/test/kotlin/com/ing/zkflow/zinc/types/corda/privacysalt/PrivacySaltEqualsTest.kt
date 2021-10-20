package com.ing.zkflow.zinc.types.corda.privacysalt

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.generateDifferentValueThan
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.PrivacySalt
import org.junit.jupiter.api.Test

class PrivacySaltEqualsTest {
    private val zincZKService = getZincZKService<PrivacySaltEqualsTest>()

    @Test
    fun `identity test`() {
        val identity = PrivacySalt(32)
        performEqualityTest(identity, identity, true)
    }

    @Test
    fun `different PrivacySalts should not be equal`() {
        val first = PrivacySalt(32)
        val second = generateDifferentValueThan(first) {
            PrivacySalt(32)
        }
        performEqualityTest(first, second, false)
    }

    private fun performEqualityTest(
        left: PrivacySalt,
        right: PrivacySalt,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
