package io.ivno.collateraltoken.zinc.types.transferstatus

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import io.ivno.collateraltoken.contract.TransferStatus

class DeserializeTransferStatusTest : DeserializationTestBase<DeserializeTransferStatusTest, TransferStatus>(
    { it.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeTransferStatusTest>()

    companion object {
        @JvmStatic
        fun testData() = TransferStatus.values()
    }
}
