package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.testing.assertRoundTripSucceeds
import com.ing.zknotary.testing.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test

class SecureHashSerializerTest {
    @Serializable
    data class Data(val value: @Contextual SecureHash)

    @Test
    fun `SecureHash serializer`() {
        assertRoundTripSucceeds(SecureHash.allOnesHash)
        assertSameSize(SecureHash.allOnesHash, SecureHash.zeroHash)
    }

    @Test
    fun `SecureHash as part of structure serializer`() {
        assertRoundTripSucceeds(Data(SecureHash.allOnesHash))
        assertSameSize(Data(SecureHash.allOnesHash), Data(SecureHash.zeroHash))
    }
}
