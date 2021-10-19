package com.ing.zkflow.zinc.types.java.collection.int

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.zkp.proveTimed
import com.ing.zkflow.testing.zkp.setupTimed
import com.ing.zkflow.testing.zkp.verifyTimed
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class CollectionEqualsTest {
    private val zincZKService = getZincZKService<CollectionEqualsTest>()

    init {
        zincZKService.setupTimed()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `zinc equals smoke test for collections of Int`(testList1: List<Int>, testList2: List<Int>) {
        val input = buildJsonObject {
            put("left", testList1.toJsonObject(3))
            put("right", testList2.toJsonObject(3))
        }.toString()
        val expected = "\"${if (testList1 == testList2) 0 else 1}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    companion object {
        private val intList = listOf(0, 1, 2)
        private val intListWithDifferentSize = listOf(0, 1)
        private val intListWithDifferentElements = listOf(0, 2, 3)
        private val intListWithDifferentOrderOfElements = listOf(0, 2, 1)

        @JvmStatic
        fun testData() = listOf(
            Arguments.of(intList, intList),
            Arguments.of(intList, intListWithDifferentSize),
            Arguments.of(intList, intListWithDifferentElements),
            Arguments.of(intList, intListWithDifferentOrderOfElements),
        )
    }
}
