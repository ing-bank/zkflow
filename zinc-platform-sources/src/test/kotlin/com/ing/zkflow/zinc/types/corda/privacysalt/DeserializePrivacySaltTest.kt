package com.ing.zkflow.zinc.types.corda.privacysalt

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toWitness
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PrivacySalt
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializePrivacySaltTest {
    private val zincZKService = getZincZKService<DeserializePrivacySaltTest>()

    private val privacySalt = PrivacySalt(32)

    @Test
    fun `a PrivacySalt should be deserialized correctly`() {
        val data = Data(privacySalt)
        val witness = toWitness(data)
        val expected = data.data.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    private data class Data(
        val data: @Contextual PrivacySalt
    )
}
