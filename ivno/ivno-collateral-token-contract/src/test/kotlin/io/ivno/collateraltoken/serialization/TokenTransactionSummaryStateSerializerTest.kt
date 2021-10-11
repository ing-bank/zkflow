package io.ivno.collateraltoken.serialization

import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import io.dasl.contracts.v1.token.TokenTransactionSummary.State
import io.ivno.collateraltoken.zinc.types.anotherState
import io.ivno.collateraltoken.zinc.types.state
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class TokenTransactionSummaryStateSerializerTest {
    @Serializable
    data class Data(val value: @Contextual State)

    private val serializersModule = IvnoSerializers.serializersModule

    @Test
    fun `serialize and deserialize TokenTransactionSummary$State directly`() {
        assertRoundTripSucceeds(state, serializersModule)
        assertSameSize(state, anotherState, serializersModule)
    }

    @Test
    fun `serialize and deserialize TokenTransactionSummary$State`() {
        val data1 = Data(state)
        val data2 = Data(anotherState)

        assertRoundTripSucceeds(data1, serializersModule)
        assertSameSize(data1, data2, serializersModule)
    }
}
