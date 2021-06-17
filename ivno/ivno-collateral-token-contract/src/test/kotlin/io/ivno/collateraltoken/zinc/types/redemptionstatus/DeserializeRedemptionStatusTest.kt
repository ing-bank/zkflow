package io.ivno.collateraltoken.zinc.types.redemptionstatus

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import io.ivno.collateraltoken.contract.RedemptionStatus

class DeserializeRedemptionStatusTest :
    DeserializationTestBase<DeserializeRedemptionStatusTest, RedemptionStatus>(
        { it.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeRedemptionStatusTest>()

    companion object {
        @JvmStatic
        fun testData() = RedemptionStatus.values()
    }
}
