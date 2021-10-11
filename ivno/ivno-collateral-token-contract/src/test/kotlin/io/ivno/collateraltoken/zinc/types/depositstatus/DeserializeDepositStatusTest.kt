package io.ivno.collateraltoken.zinc.types.depositstatus

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
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
