package io.ivno.collateraltoken.zinc.types.tokentransactionsummary.nettedaccountamount

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenTransactionSummary.NettedAccountAmount
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.nettedAccountAmount
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

class DeserializeNettedAccountAmountTest :
    DeserializationTestBase<DeserializeNettedAccountAmountTest, DeserializeNettedAccountAmountTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeNettedAccountAmountTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual NettedAccountAmount)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(nettedAccountAmount),
        )
    }
}