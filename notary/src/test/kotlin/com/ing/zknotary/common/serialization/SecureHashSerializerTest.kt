package com.ing.zknotary.common.serialization

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test

@ExperimentalSerializationApi
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
