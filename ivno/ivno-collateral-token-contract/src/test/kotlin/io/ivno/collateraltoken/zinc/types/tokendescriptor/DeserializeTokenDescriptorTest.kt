package io.ivno.collateraltoken.zinc.types.tokendescriptor

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.ivno.collateraltoken.zinc.types.tokenDescriptor
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

class DeserializeTokenDescriptorTest :
DeserializationTestBase<DeserializeTokenDescriptorTest, DeserializeTokenDescriptorTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeTokenDescriptorTest>()

    @ExperimentalSerializationApi
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
