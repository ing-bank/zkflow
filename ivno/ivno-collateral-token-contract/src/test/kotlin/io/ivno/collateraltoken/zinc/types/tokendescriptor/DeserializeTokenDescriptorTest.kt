package io.ivno.collateraltoken.zinc.types.tokendescriptor

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.ivno.collateraltoken.zinc.types.tokenDescriptor
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

class DeserializeTokenDescriptorTest :
DeserializationTestBase<DeserializeTokenDescriptorTest, DeserializeTokenDescriptorTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeTokenDescriptorTest>()

        override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual TokenDescriptor)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(tokenDescriptor),
        )
    }
}
