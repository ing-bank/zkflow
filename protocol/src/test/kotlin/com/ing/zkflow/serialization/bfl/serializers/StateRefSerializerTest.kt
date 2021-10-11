package com.ing.zkflow.serialization.bfl.serializers

import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test

class StateRefSerializerTest {
    @Serializable
    data class Data(val value: @Contextual StateRef)

    @Test
    fun `StateRef serializer`() {
        assertRoundTripSucceeds(StateRef(SecureHash.allOnesHash, 1))
        assertSameSize(StateRef(SecureHash.allOnesHash, 1), StateRef(SecureHash.zeroHash, 0))
    }

    @Test
    fun `StateRef as part of structure serializer`() {
        assertRoundTripSucceeds(Data(StateRef(SecureHash.allOnesHash, 1)))
        assertSameSize(
            Data(StateRef(SecureHash.allOnesHash, 1)),
            Data(StateRef(SecureHash.zeroHash, 1))
        )
    }
}
