package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.testing.assertRoundTripSucceeds
import com.ing.zkflow.testing.assertSameSize
import io.onixlabs.corda.bnms.contract.Setting
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test

class SettingSerializerTest {
    @Serializable
    data class Data(
        @FixedLength([7])
        @Serializable(with = SettingSerializer::class)
        val setting1: Setting<String>,
        @Serializable(with = SettingSerializer::class)
        val setting2: Setting<Int>
    )

    private val settingWitString1 : Setting<String> = Setting("Property 1", "Value 1")
    private val settingWitString2 : Setting<String> = Setting("Property 2", "Value 2")

    private val settingWitInt1 : Setting<Int> = Setting("Property 1", 1)
    private val settingWitInt2 : Setting<Int> = Setting("Property 2", 2)

    @Test
    fun `serialize and deserialize Setting with String directly`() {
        assertRoundTripSucceeds(settingWitString1, strategy = SettingSerializer(String.serializer()), outerFixedLength = intArrayOf(7))
        assertSameSize(settingWitString1, settingWitString2, strategy = SettingSerializer(String.serializer()), outerFixedLength = intArrayOf(7))
    }

    @Test
    fun `serialize and deserialize Setting with Int directly`() {
        assertRoundTripSucceeds(settingWitInt1, strategy = SettingSerializer(Int.serializer()))
        assertSameSize(settingWitInt1, settingWitInt2, strategy = SettingSerializer(Int.serializer()))
    }

    @Test
    fun `serialize and deserialize Setting with Int`() {
        val data1 = Data(settingWitString1, settingWitInt1)
        val data2 = Data(settingWitString2, settingWitInt2)

        assertRoundTripSucceeds(data1)
        assertSameSize(data1, data2)
    }
}