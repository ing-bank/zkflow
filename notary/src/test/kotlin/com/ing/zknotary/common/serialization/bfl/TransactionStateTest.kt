package com.ing.zknotary.common.serialization.bfl

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.TransactionStateSerializer
import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import com.ing.zknotary.testing.fixtures.state.DummyState
import net.corda.core.crypto.Crypto
import org.junit.jupiter.api.Test

class TransactionStateTest {
    private val localSerializers = CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.DEFAULT_SIGNATURE_SCHEME)
    private val strategy = TransactionStateSerializer(DummyState.serializer())

    @Test
    fun `TransactionState serializes and deserializes`() {
        val state1 = DummyState.newTxState()
        val state2 = DummyState.newTxState()

        assertRoundTripSucceeds(state1, localSerializers, strategy)
        assertSameSize(state1, state2, localSerializers, strategy)
    }
}
