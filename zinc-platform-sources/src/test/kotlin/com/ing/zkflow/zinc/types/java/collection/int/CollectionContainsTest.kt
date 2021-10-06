package com.ing.zkflow.zinc.types.java.collection.int

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.proveTimed
import com.ing.zkflow.zinc.types.setupTimed
import com.ing.zkflow.zinc.types.toJsonObject
import com.ing.zkflow.zinc.types.verifyTimed
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    fun `zinc contains smoke test for collections Int`(testList: List<Int>, value: Int) {
        val input = buildJsonObject {
            put("left", testList.toJsonObject(3))
            put("right", "$value")
        }.toString()
        val expected = "\"${if (testList.contains(value)) 0 else 1}\""

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    companion object {
        private val intList = listOf(0, 1, 2)

        @JvmStatic
        fun testData() = listOf(
            Arguments.of(intList, 1),
            Arguments.of(intList, 3),
        )
    }
}
