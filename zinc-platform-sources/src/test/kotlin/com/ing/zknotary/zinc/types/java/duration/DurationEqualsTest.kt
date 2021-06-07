package com.ing.zknotary.zinc.types.java.duration

import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DurationEqualsTest {
    private val zincZKService = getZincZKService<DurationEqualsTest>()

    @Test
    fun `identity test`() {
        val fourtyTwo = Duration.ofSeconds(42)
        performEqualityTest(fourtyTwo, fourtyTwo, true)
    }

    @Test
    fun `durations with different seconds should not be equal`() {
        val thirteen = Duration.ofSeconds(13)
        val fourtyTwo = Duration.ofSeconds(42)
        performEqualityTest(fourtyTwo, thirteen, false)
    }

    @Test
    fun `durations with different nanos should not be equal`() {
        val thirteen = Duration.ofNanos(13)
        val fourtyTwo = Duration.ofNanos(42)
        performEqualityTest(fourtyTwo, thirteen, false)
    }

    private fun performEqualityTest(
        left: Duration,
        right: Duration,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}
