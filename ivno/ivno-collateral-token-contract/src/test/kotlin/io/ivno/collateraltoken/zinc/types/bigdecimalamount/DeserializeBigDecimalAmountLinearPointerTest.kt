package io.ivno.collateraltoken.zinc.types.bigdecimalamount

import com.ing.serialization.bfl.serializers.BigDecimalSizes
import com.ing.zkflow.serialization.bfl.corda.LinearPointerSerializer
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.serialization.BigDecimalAmountSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import java.util.UUID

class DeserializeBigDecimalAmountLinearPointerTest :
DeserializationTestBase<DeserializeBigDecimalAmountLinearPointerTest, DeserializeBigDecimalAmountLinearPointerTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeBigDecimalAmountLinearPointerTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = SerializersModule { contextual(BigDecimalAmountSerializer(LinearPointerSerializer)) }

    @Serializable
    data class Data(@BigDecimalSizes([20, 4]) val data: @Contextual BigDecimalAmount<@Contextual LinearPointer<@Contextual IvnoTokenType>>)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(BigDecimalAmount(42, LinearPointer(UniqueIdentifier(id = UUID(0, 42)), IvnoTokenType::class.java))),
        )
    }
}
