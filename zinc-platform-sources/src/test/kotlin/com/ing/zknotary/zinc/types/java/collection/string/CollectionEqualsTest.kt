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
class CollectionEqualsTest {
    private val log = loggerFor<CollectionEqualsTest>()
    private val zincZKService = getZincZKService<CollectionEqualsTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `zinc equals smoke test for collections of String`(testList1: List<String>, testList2: List<String>) {
        val input = buildJsonObject {
            put("left", testList1.toJsonObject(3, 1))
            put("right", testList2.toJsonObject(3, 1))
        }.toString()
        val expected = "\"${if (testList1 == testList2) 0 else 1}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    companion object {
        private val stringList = listOf("0", "1", "2")
        private val stringListWithDifferentSize = listOf("0", "1")
        private val stringListWithDifferentElements = listOf("0", "2", "3")
        private val stringListWithDifferentOrderOfElements = listOf("0", "2", "1")

        @JvmStatic
        fun testData() = listOf(
            Arguments.of(stringList, stringList),
            Arguments.of(stringList, stringListWithDifferentSize),
            Arguments.of(stringList, stringListWithDifferentElements),
            Arguments.of(stringList, stringListWithDifferentOrderOfElements),
        )
    }
}
