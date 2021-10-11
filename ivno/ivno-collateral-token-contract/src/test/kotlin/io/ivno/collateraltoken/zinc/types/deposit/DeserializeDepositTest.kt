package io.ivno.collateraltoken.zinc.types.deposit

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.deposit
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

class DeserializeDepositTest :
DeserializationTestBase<DeserializeDepositTest, DeserializeDepositTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeDepositTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: Deposit)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            return listOf(
                Data(deposit),
            )
        }
    }
}
