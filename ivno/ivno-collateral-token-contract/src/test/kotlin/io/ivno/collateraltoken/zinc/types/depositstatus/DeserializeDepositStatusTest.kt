package io.ivno.collateraltoken.zinc.types.depositstatus

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import io.ivno.collateraltoken.contract.DepositStatus

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
