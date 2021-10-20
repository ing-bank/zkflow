package io.ivno.collateraltoken.zinc.types.redemptionstatus

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import io.ivno.collateraltoken.contract.RedemptionStatus

class DeserializeRedemptionStatusTest : DeserializationTestBase<DeserializeRedemptionStatusTest, RedemptionStatus>(
    { it.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeRedemptionStatusTest>()

    companion object {
        @JvmStatic
        fun testData() = RedemptionStatus.values()
    }
}
