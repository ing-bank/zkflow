package io.ivno.collateraltoken.zinc.types.transfer

import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSerializer
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.serialization.BigDecimalAmountSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.ivno.collateraltoken.zinc.types.transfer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

class DeserializeTransferTest :
DeserializationTestBase<DeserializeTransferTest, DeserializeTransferTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeTransferTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = SerializersModule {
        contextual(BigDecimalAmountSerializer(LinearPointerSerializer))
    }

    @Serializable
    data class Data(val data: Transfer)

    companion object {
        @JvmStatic
        fun testData(): List<Data> {
            return listOf(
                Data(transfer),
            )
        }
    }
}
