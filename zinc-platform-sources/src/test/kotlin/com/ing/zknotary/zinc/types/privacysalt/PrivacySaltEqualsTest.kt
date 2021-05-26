package com.ing.zknotary.zinc.types.privacysalt

import com.ing.zknotary.zinc.types.generateDifferentValueThan
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.PrivacySalt
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
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
