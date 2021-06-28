package io.ivno.collateraltoken.zinc.types.redemption

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.redemption
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

class DeserializeRedemptionTest :
DeserializationTestBase<DeserializeRedemptionTest, DeserializeRedemptionTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeRedemptionTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: Redemption)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            return listOf(
                Data(redemption)
            )
        }
    }
}
