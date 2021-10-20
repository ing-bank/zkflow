package com.ing.zkflow.zinc.types.java.date

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toWitness
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date

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
