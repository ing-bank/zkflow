package com.ing.zkflow.zinc.types.corda.linearpointer

import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.testing.toJsonArray
import com.ing.zkflow.zinc.types.proveTimed
import com.ing.zkflow.zinc.types.setupTimed
import com.ing.zkflow.zinc.types.verifyTimed
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.ExperimentalTime

@ExperimentalTime
class SerializedLinearPointerEqualsTest {
    private val log = loggerFor<SerializedLinearPointerEqualsTest>()
    private val zincZKService = getZincZKService<SerializedLinearPointerEqualsTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `a LinearPointer should be equal to itself`() {
        val data = Data(LinearPointer(getSomeId(), MyLinearState::class.java, true))
        performEqualityTest(data, data)
    }

    @Test
    fun `two LinearPointers should not be equal when the pointer is different`() {
        val data1 = Data(LinearPointer(getSomeId(), MyLinearState::class.java, true))
        val data2 = Data(LinearPointer(someDifferentId(), MyLinearState::class.java, true))
        performEqualityTest(data1, data2)
    }

    @Test
    fun `two LinearPointers should be equal when the pointer is equal, but not the externalId`() {
        val data1 = Data(LinearPointer(getSomeId(), MyLinearState::class.java, true))
        val data2 = Data(LinearPointer(someOtherSameId(), MyLinearState::class.java, true))
        performEqualityTest(data1, data2)
    }

    @Test
    fun `two LinearPointers should be equal to when the pointer is equal, with absent externalIds`() {
        val data1 = Data(LinearPointer(getSomeId(), MyLinearState::class.java, true))
        val data2 = Data(LinearPointer(UniqueIdentifier(id = UUID(1L, 42L)), MyLinearState::class.java, true))
        performEqualityTest(data1, data2)
    }

    private fun performEqualityTest(
        data1: Data,
        data2: Data
    ) {
        val witness = toWitness(data1, data2)
        val areEqual = data1.id == data2.id
        val expected = "$areEqual"

        zincZKService.proveTimed(witness, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }

    private fun toWitness(left: Data, right: Data): String {
        val bytes = serialize(left, serializersModule = CordaSerializers.module) +
            serialize(right, serializersModule = CordaSerializers.module)
        return "{\"witness\":${bytes.toJsonArray()}}"
    }

    @Serializable
    private data class MyLinearState(
        override val participants: List<@Contextual AbstractParty>,
        override val linearId: @Contextual UniqueIdentifier
    ) : LinearState

    @Serializable
    private data class Data(
        val id: @Contextual LinearPointer<MyLinearState>
    )

    private fun getSomeId() = UniqueIdentifier("some.id", UUID(1L, 42L))

    private fun someOtherSameId() = UniqueIdentifier("some.other.id", UUID(1L, 42L))

    private fun someDifferentId() = UniqueIdentifier("some.id", UUID(1L, 43L))
}
