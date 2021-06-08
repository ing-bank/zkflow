package io.ivno.collateraltoken.zinc.types.depositstatus

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.DepositStatus
import io.ivno.collateraltoken.zinc.types.toZincJson

class DeserializeDepositStatusTest :
    DeserializationTestBase<DeserializeDepositStatusTest, DepositStatus>(
        { it.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeDepositStatusTest>()

    companion object {
        @JvmStatic
        fun testData() = DepositStatus.values()
    }
}
