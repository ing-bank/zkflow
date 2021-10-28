package com.ing.zkflow.zinc.types.java.date

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import java.util.Date

class DateEqualsTest {
    private val zincZKService = getZincZKService<DateEqualsTest>()

    @Test
    fun `identity test`() {
        val now = Date(42)
        performEqualityTest(now, now, true)
    }

    @Test
    fun `different dates should not be equal`() {
        performEqualityTest(Date(42), Date(13), false)
    }

    private fun performEqualityTest(
        left: Date,
        right: Date,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }
}