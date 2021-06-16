package io.ivno.collateraltoken.zinc.types.role

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.serialization.RoleSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.Role
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual


class DeserializeRoleTest :
    DeserializationTestBase<DeserializeRoleTest, DeserializeRoleTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeRoleTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = SerializersModule { contextual(RoleSerializer) }

    @Serializable
    data class Data(val data: @Contextual Role)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(Role("Thief")),
        )
    }
}