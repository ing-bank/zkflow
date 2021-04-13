package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.testing.roundTrip
import com.ing.zknotary.testing.sameSize
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
        roundTrip(StateRef(SecureHash.allOnesHash, 1))
        sameSize(StateRef(SecureHash.allOnesHash, 1), StateRef(SecureHash.zeroHash, 0))
    }

    @Test
    fun `StateRef as part of structure serializer`() {
        roundTrip(Data(StateRef(SecureHash.allOnesHash, 1)))
        sameSize(
            Data(StateRef(SecureHash.allOnesHash, 1)),
            Data(StateRef(SecureHash.zeroHash, 1))
        )
    }
}
