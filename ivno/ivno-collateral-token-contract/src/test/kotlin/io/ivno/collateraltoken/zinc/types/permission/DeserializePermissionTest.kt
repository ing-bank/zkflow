package io.ivno.collateraltoken.zinc.types.permission

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.serialization.PermissionSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.Permission
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

class DeserializePermissionTest :
    DeserializationTestBase<DeserializePermissionTest, DeserializePermissionTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializePermissionTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = SerializersModule { contextual(PermissionSerializer) }

    @Serializable
    data class Data(val data: @Contextual Permission)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(Permission("Permission")),
        )
    }
}