package com.ing.zkflow.zinc.types.java.instant

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toWitness
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.time.Instant

class DeserializeInstantTest {
    private val zincZKService = getZincZKService<DeserializeInstantTest>()

    @Test
    fun `an Instance should be deserialized correctly`() {
        val data = Data(Instant.ofEpochSecond(42, 79))
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    private data class Data(
        val data: @Contextual Instant
    )
}
