package io.ivno.collateraltoken.zinc.types.bigdecimalamount

import com.ing.serialization.bfl.serializers.BigDecimalSizes
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.serialization.BigDecimalAmountSerializer
import io.ivno.collateraltoken.serialization.TokenDescriptorSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.ivno.collateraltoken.zinc.types.tokenDescriptor
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

class DeserializeBigDecimalAmountTokenDescriptorTest :
    DeserializationTestBase<DeserializeBigDecimalAmountTokenDescriptorTest, DeserializeBigDecimalAmountTokenDescriptorTest.Data>(
        { it.data.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBigDecimalAmountTokenDescriptorTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = SerializersModule {
        contextual(BigDecimalAmountSerializer(TokenDescriptorSerializer))
        contextual(TokenDescriptorSerializer)
    }

    @Serializable
    data class Data(@BigDecimalSizes([20, 4]) val data: @Contextual BigDecimalAmount<@Contextual TokenDescriptor>)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(BigDecimalAmount(42, tokenDescriptor)),
        )
    }
}
