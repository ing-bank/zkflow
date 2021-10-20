package com.ing.zkflow.zinc.types.java.string

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.toJsonArray
import com.ing.zkflow.testing.zkp.proveTimed
import com.ing.zkflow.testing.zkp.setupTimed
import com.ing.zkflow.testing.zkp.verifyTimed
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

internal class SerializedStringCompareTest {
    private val zincZKService = getZincZKService<SerializedStringCompareTest>()

    init {
        zincZKService.setupTimed()
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc equals with equal strings`() {
        val data = Data("some.string", "some.string")
        val input = data.toWitnessJson()
        val expected = "\"${data.getCompareResult()}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc equals with different strings, first smaller`() {
        val data = Data("Some.String", "some.string")
        val input = data.toWitnessJson()
        val expected = "\"${data.getCompareResult()}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Test
    fun `zinc equals with different strings, first larger`() {
        val data = Data("some.string", "some.strinG")
        val input = data.toWitnessJson()
        val expected = "\"${data.getCompareResult()}\""

        zincZKService.proveTimed(input).let {
            zincZKService.verifyTimed(it, expected)
        }
    }

    @Serializable
    private data class Data(
        @FixedLength([32])
        val first: String,
        @FixedLength([32])
        val second: String
    ) {
        fun toWitnessJson(): String {
            val dataJson = serialize(this, serializer()).toJsonArray()
            return "{\"witness\":$dataJson}"
        }

        fun getCompareResult(): Int {
            val result = first.compareTo(second)
            return when {
                result < 0 -> -1
                result > 0 -> 1
                else -> 0
            }
        }
    }
}
