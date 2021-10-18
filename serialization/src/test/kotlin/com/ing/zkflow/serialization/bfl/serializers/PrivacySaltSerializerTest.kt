package com.ing.zkflow.serialization.bfl.serializers

import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PrivacySalt
import org.junit.jupiter.api.Test

class PrivacySaltSerializerTest {
    @Serializable
    data class Data(val value: @Contextual PrivacySalt)

    @Test
    fun `PrivacySalt serializer`() {
        val bytes1 = ByteArray(32) { i -> i.toByte() }
        val bytes2 = ByteArray(32) { i -> (i + 1).toByte() }

        assertRoundTripSucceeds(PrivacySalt())
        assertSameSize(PrivacySalt(bytes1), PrivacySalt(bytes2))
    }

    @Test
    fun `PrivacySalt as part of structure serializer`() {
        val bytes1 = ByteArray(32) { i -> i.toByte() }
        val bytes2 = ByteArray(32) { i -> (i + 1).toByte() }

        assertRoundTripSucceeds(Data(PrivacySalt()))
        assertSameSize(Data(PrivacySalt(bytes1)), Data(PrivacySalt(bytes2)))
    }
}
