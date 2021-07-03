package com.ing.zknotary.common.serialization.bfl

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.TransactionStateSerializer
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import com.ing.zknotary.testing.fixtures.state.DummyState
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import org.junit.jupiter.api.Test

class TransactionStateTest {
    private val strategy = TransactionStateSerializer(DummyState.serializer())

    @Test
    fun `TransactionState serializes and deserializes`() {
        val state1 = DummyState.newTxState()
        val state2 = DummyState.newTxState()

        assertRoundTripSucceeds(
            state1, strategy = strategy,
            serializers = CordaSerializers.module + SerializersModule {
                contextual(strategy)
            }
        )
        assertSameSize(
            state1, state2, strategy = strategy,
            serializers = CordaSerializers.module + SerializersModule {
                contextual(strategy)
            }
        )
    }
}
