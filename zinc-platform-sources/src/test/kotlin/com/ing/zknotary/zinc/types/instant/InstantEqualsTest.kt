package com.ing.zknotary.zinc.types.instant

import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.time.ExperimentalTime

@ExperimentalTime
class InstantEqualsTest {
    private val zincZKService = getZincZKService<InstantEqualsTest>()

    @Test
    fun `identity test`() {
        val fourtyTwo = Instant.ofEpochSecond(42)
        performEqualityTest(fourtyTwo, fourtyTwo, true)
    }

    @Test
    fun `instants with different seconds should not be equal`() {
        val thirteen = Instant.ofEpochSecond(13)
        val fourtyTwo = Instant.ofEpochSecond(42)
        performEqualityTest(fourtyTwo, thirteen, false)
    }

    @Test
    fun `instants with different nanos should not be equal`() {
        val thirteen = Instant.ofEpochSecond(0, 13)
        val fourtyTwo = Instant.ofEpochSecond(0, 42)
        performEqualityTest(fourtyTwo, thirteen, false)
    }

    private fun performEqualityTest(
        left: Instant,
        right: Instant,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
