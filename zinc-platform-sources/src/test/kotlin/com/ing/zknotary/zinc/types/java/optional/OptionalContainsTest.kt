package com.ing.zknotary.zinc.types.java.optional

import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.proveTimed
import com.ing.zknotary.zinc.types.setupTimed
import com.ing.zknotary.zinc.types.verifyTimed
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.ExperimentalTime

@ExperimentalTime
class OptionalContainsTest {
    private val log = loggerFor<OptionalContainsTest>()
    private val zincZKService = getZincZKService<OptionalContainsTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `zinc contains smoke test`(dataPair: Triple<Int, Int, Boolean>) {
        val left = dataPair.first
        val right = dataPair.second
        val leftIsNone = dataPair.third

        val input = buildJsonObject {
            put("left", "$left")
            put("right", "$right")
            put("is_none", leftIsNone)
        }.toString()
        val expected = "\"${if (left == right && !leftIsNone) 0 else 1}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Triple(42, 42, false),
            Triple(42, 42, true),
            Triple(42, 43, false)
        )
    }
}
