package io.ivno.collateraltoken.zinc.types.role

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.Role
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


class DeserializeRoleTest :
    DeserializationTestBase<DeserializeRoleTest, DeserializeRoleTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeRoleTest>()

        override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual Role)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(Role("Thief")),
        )
    }
}