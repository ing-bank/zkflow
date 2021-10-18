package io.ivno.collateraltoken.zinc.types.setting

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.SettingSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.Setting
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

class DeserializeSettingTest :
    DeserializationTestBase<DeserializeSettingTest, DeserializeSettingTest.Data>(
        {
            it.data.toZincJson(VALUE_LENGTH)
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeSettingTest>()

        override fun getSerializersModule() = SerializersModule { contextual(SettingSerializer(String.serializer())) }

    @Serializable
    data class Data(@FixedLength([VALUE_LENGTH]) val data: @Contextual Setting<String>)

    companion object {
        const val VALUE_LENGTH = 7

        @JvmStatic
        fun testData() = listOf(Data(Setting("Property","Value")))
    }
}