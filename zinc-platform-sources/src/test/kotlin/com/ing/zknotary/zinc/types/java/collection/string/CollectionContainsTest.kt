package com.ing.zknotary.zinc.types.java.collection.string

import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.proveTimed
import com.ing.zknotary.zinc.types.setupTimed
import com.ing.zknotary.zinc.types.toJsonObject
import com.ing.zknotary.zinc.types.verifyTimed
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.ExperimentalTime

@ExperimentalTime
class CollectionContainsTest {
    private val log = loggerFor<CollectionContainsTest>()
    private val zincZKService = getZincZKService<CollectionContainsTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `zinc contains smoke test for collections of String`(testList: List<String>, value: String) {
        val input = buildJsonObject {
            put("left", testList.toJsonObject(3, 1))
            put("right", value.toJsonObject(1))
        }.toString()
        val expected = "\"${if (testList.contains(value)) 0 else 1}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    companion object {
        private val stringList = listOf("0", "1", "2")

        @JvmStatic
        fun testData() = listOf(
            Arguments.of(stringList, "1"),
            Arguments.of(stringList, "3"),
        )
    }
}
