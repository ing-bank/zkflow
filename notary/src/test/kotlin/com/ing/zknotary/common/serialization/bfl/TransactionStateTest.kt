package com.ing.zknotary.common.serialization.bfl

import com.ing.zknotary.common.serialization.bfl.serializers.TransactionStateSerializer
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import com.ing.zknotary.testing.fixtures.state.DummyState
import org.junit.jupiter.api.Test

class TransactionStateTest {
    private val strategy = TransactionStateSerializer(DummyState.serializer())

    @Test
    fun `TransactionState serializes and deserializes`() {
        val state1 = DummyState.newTxState()
        val state2 = DummyState.newTxState()

        assertRoundTripSucceeds(state1, strategy = strategy)
        assertSameSize(state1, state2, strategy = strategy)
    }
}
