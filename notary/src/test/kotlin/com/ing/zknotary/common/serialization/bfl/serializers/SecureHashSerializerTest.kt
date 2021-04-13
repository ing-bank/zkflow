package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.testing.roundTrip
import com.ing.zknotary.testing.sameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test

class SecureHashSerializerTest {
    @Serializable
    data class Data(val value: @Contextual SecureHash)

    @Test
    fun `SecureHash serializer`() {
        roundTrip(SecureHash.allOnesHash)
        sameSize(SecureHash.allOnesHash, SecureHash.zeroHash)
    }

    @Test
    fun `SecureHash as part of structure serializer`() {
        roundTrip(Data(SecureHash.allOnesHash))
        sameSize(Data(SecureHash.allOnesHash), Data(SecureHash.zeroHash))
    }
}
