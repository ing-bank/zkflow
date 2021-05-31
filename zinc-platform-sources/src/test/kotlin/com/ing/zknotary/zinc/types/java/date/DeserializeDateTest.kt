package com.ing.zknotary.zinc.types.java.date

import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toWitness
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeDateTest {
    private val zincZKService = getZincZKService<DeserializeDateTest>()

    @Test
    fun `a Date should be deserialized correctly`() {
        val data = Data(Date.from(Instant.now()))
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    private data class Data(
        val data: @Contextual Date
    )
}