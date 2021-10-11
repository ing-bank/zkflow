package com.ing.zkflow.serialization.bfl.corda

import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import org.junit.jupiter.api.Test

internal class LinearPointerSerializerTest {
    @Serializable
    private data class MyLinearState(
        override val participants: List<@Contextual AbstractParty>,
        override val linearId: @Contextual UniqueIdentifier
    ) : LinearState
    @Serializable
    private data class MyOtherLinearState(
        override val participants: List<@Contextual AbstractParty>,
        override val linearId: @Contextual UniqueIdentifier
    ) : LinearState
    @Serializable
    private data class Data(val data: @Contextual LinearPointer<out LinearState>)

    @Test
    fun `different LinearPointers should have the same size`() {
        val data1 = Data(LinearPointer(pointer = UniqueIdentifier(), type = MyLinearState::class.java))
        val data2 = Data(LinearPointer(pointer = UniqueIdentifier(), type = MyOtherLinearState::class.java))
        assertSameSize(data1, data2, serializers = CordaSerializers.module)
    }

    @Test
    fun `LinearPointer with MyLinearState should be deserialized correctly`() {
        val data1 = Data(LinearPointer(pointer = UniqueIdentifier(), type = MyLinearState::class.java))
        assertRoundTripSucceeds(data1, CordaSerializers.module).data.type shouldBe MyLinearState::class.java
    }

    @Test
    fun `LinearPointer with MyOtherLinearState should be deserialized correctly`() {
        val data2 = Data(LinearPointer(pointer = UniqueIdentifier(), type = MyOtherLinearState::class.java))
        assertRoundTripSucceeds(data2, CordaSerializers.module).data.type shouldBe MyOtherLinearState::class.java
    }

    @Test
    fun `validate LinearPointer with different types`() {
        val pointer = UniqueIdentifier()
        val data1 = Data(LinearPointer(pointer = pointer, type = MyLinearState::class.java))
        val deserializedData1 = assertRoundTripSucceeds(data1, CordaSerializers.module)

        val data2 = Data(LinearPointer(pointer = pointer, type = MyOtherLinearState::class.java))
        val deserializedData2 = assertRoundTripSucceeds(data2, CordaSerializers.module)

        // LinearPointers have the same pointer, so they're equal
        data1 shouldBe data2
        // The types of the deserialized values should be different
        data1.data.type shouldNotBe data2.data.type
    }
}
