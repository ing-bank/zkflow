package com.ing.zknotary.zinc.types.linearpointer

import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toWitness
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeLinearPointerTest {
    private val zincZKService = getZincZKService<DeserializeLinearPointerTest>()

    @Test
    fun `a LinearPointer should be deserialized correctly`() {
        val data = Data(LinearPointer(getSomeId(), MyLinearState::class.java, true))
        val witness = toWitness(data)

        val expected = data.pointer.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    private data class MyLinearState(
        override val participants: List<@Contextual AbstractParty>,
        override val linearId: @Contextual UniqueIdentifier
    ) : LinearState

    @Serializable
    private data class Data(
        val pointer: @Contextual LinearPointer<MyLinearState>
    )

    private fun getSomeId() = UniqueIdentifier(
        externalId = "some.id",
        id = UUID(0L, 42L)
    )
}
