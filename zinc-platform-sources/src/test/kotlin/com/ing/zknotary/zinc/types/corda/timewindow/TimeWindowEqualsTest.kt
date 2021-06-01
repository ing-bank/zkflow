package com.ing.zknotary.zinc.types.corda.timewindow

import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.TimeWindow
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.ExperimentalTime

@ExperimentalTime
class TimeWindowEqualsTest {
    private val zincZKService = getZincZKService<TimeWindowEqualsTest>()

    @Test
    fun `identity test closed range`() {
        val now = TimeWindow.between(now, inFiveMinutes)
        performEqualityTest(now, now, true)
    }

    @Test
    fun `identity test open start range`() {
        val now = TimeWindow.untilOnly(inFiveMinutes)
        performEqualityTest(now, now, true)
    }

    @Test
    fun `identity test open end range`() {
        val now = TimeWindow.fromOnly(now)
        performEqualityTest(now, now, true)
    }

    @Test
    fun `different TimeWindows should not be equal`() {
        performEqualityTest(TimeWindow.between(now, inFiveMinutes), TimeWindow.between(now, inTenMinutes), false)
    }

    @Test
    fun `different TimeWindows with open end range should not be equal`() {
        performEqualityTest(TimeWindow.between(now, inFiveMinutes), TimeWindow.fromOnly(now), false)
    }

    @Test
    fun `different TimeWindows with open start range should not be equal`() {
        performEqualityTest(TimeWindow.between(now, inFiveMinutes), TimeWindow.untilOnly(inFiveMinutes), false)
    }

    private fun performEqualityTest(
        left: TimeWindow,
        right: TimeWindow,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        private val now = Instant.now()
        private val inFiveMinutes = now.plus(5, ChronoUnit.MINUTES)
        private val inTenMinutes = now.plus(10, ChronoUnit.MINUTES)
    }
}
