package com.ing.zkflow.zinc.types.java.zoneddatetime

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ZonedDateTimeEqualsTest {
    private val zincZKService = getZincZKService<ZonedDateTimeEqualsTest>()

    @Test
    fun `identity test with ZoneOffset`() {
        performEqualityTest(nowOffset, nowOffset, true)
    }

    @Test
    fun `identity test with ZoneId`() {
        performEqualityTest(base, base, true)
    }

    @Test
    fun `ZonedDateTimes with different years should not be equal`() {
        performEqualityTest(base, basePlusYear, false)
    }

    @Test
    fun `ZonedDateTimes with different months should not be equal`() {
        performEqualityTest(base, basePlusMonth, false)
    }

    @Test
    fun `ZonedDateTimes with different days should not be equal`() {
        performEqualityTest(base, basePlusDay, false)
    }

    @Test
    fun `ZonedDateTimes with different hours should not be equal`() {
        performEqualityTest(base, basePlusHour, false)
    }

    @Test
    fun `ZonedDateTimes with different minutes should not be equal`() {
        performEqualityTest(base, basePlusMinute, false)
    }

    @Test
    fun `ZonedDateTimes with different seconds should not be equal`() {
        performEqualityTest(base, basePlusSecond, false)
    }

    @Test
    fun `ZonedDateTimes with different nanos should not be equal`() {
        performEqualityTest(base, basePlusNano, false)
    }

    @Test
    fun `ZonedDateTimes with different zones should not be equal`() {
        performEqualityTest(base, baseWithCET, false)
    }

    private fun performEqualityTest(
        left: ZonedDateTime,
        right: ZonedDateTime,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        val nowOffset: ZonedDateTime = ZonedDateTime.now(ZoneOffset.ofTotalSeconds(42))
        val base: ZonedDateTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"))
        val basePlusYear: ZonedDateTime = ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"))
        val basePlusMonth: ZonedDateTime = ZonedDateTime.of(0, 2, 1, 0, 0, 0, 0, ZoneId.of("GMT"))
        val basePlusDay: ZonedDateTime = ZonedDateTime.of(0, 1, 2, 0, 0, 0, 0, ZoneId.of("GMT"))
        val basePlusHour: ZonedDateTime = ZonedDateTime.of(0, 1, 1, 1, 0, 0, 0, ZoneId.of("GMT"))
        val basePlusMinute: ZonedDateTime = ZonedDateTime.of(0, 1, 1, 0, 1, 0, 0, ZoneId.of("GMT"))
        val basePlusSecond: ZonedDateTime = ZonedDateTime.of(0, 1, 1, 0, 0, 1, 0, ZoneId.of("GMT"))
        val basePlusNano: ZonedDateTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 1, ZoneId.of("GMT"))
        val baseWithCET: ZonedDateTime = ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneId.of("CET"))
    }
}
