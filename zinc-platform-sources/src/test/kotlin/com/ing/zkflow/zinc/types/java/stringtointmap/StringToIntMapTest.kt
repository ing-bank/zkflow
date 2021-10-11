package com.ing.zkflow.zinc.types.java.stringtointmap

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.toJsonArray
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class StringToIntMapTest {
    private val zincZKService = getZincZKService<StringToIntMapTest>()

    @Test
    fun `retrieve existing value from map`() {
        runTest("three", 3)
    }

    @Test
    fun `retrieve non existing value from map`() {
        runTest("hundred", -1)
    }

    private fun runTest(key: String, expectedValue: Int) {
        val dataJson = serialize(testData, Data.serializer()).toJsonArray()
        val witness = buildJsonObject {
            put("data", dataJson)
            put("key", key.toJsonObject(5))
        }
        val expected = buildJsonObject {
            put("value", JsonPrimitive("$expectedValue"))
        }
        zincZKService.run(witness.toString(), expected.toString())
    }

    companion object {
        @Serializable
        data class Data(
            @FixedLength([6, 5])
            val data: Map<String, Int>
        )

        val testData = Data(
            mapOf(
                "one" to 1,
                "two" to 2,
                "three" to 3,
                "four" to 4,
                "five" to 5,
            )
        )
    }
}
