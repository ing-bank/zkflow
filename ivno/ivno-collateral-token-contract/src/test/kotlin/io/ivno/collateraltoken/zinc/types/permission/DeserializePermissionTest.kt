package io.ivno.collateraltoken.zinc.types.permission

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.Permission
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

class DeserializePermissionTest :
    DeserializationTestBase<DeserializePermissionTest, DeserializePermissionTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializePermissionTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual Permission)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(Permission("Permission")),
        )
    }
}