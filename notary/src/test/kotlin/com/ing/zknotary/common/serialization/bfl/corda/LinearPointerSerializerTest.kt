package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
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
    fun `LinearPointer should equal the original after serialization and deserialization`() {
        val data = Data(LinearPointer(pointer = UniqueIdentifier(), type = MyLinearState::class.java))
        assertRoundTripSucceeds(data, serializers = CordaSerializers)
    }

    @Test
    fun `different LinearPointers should have the same size`() {
        val data1 = Data(LinearPointer(pointer = UniqueIdentifier(), type = MyLinearState::class.java))
        val data2 = Data(LinearPointer(pointer = UniqueIdentifier(), type = MyOtherLinearState::class.java))
        assertSameSize(data1, data2, serializers = CordaSerializers)
    }
}
