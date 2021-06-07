package io.ivno.collateraltoken.zinc.types.transferstatus

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.TransferStatus
import io.ivno.collateraltoken.zinc.types.toZincJson

class DeserializeTransferStatusTest :
    DeserializationTestBase<DeserializeTransferStatusTest, TransferStatus>(
        { it.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeTransferStatusTest>()

    companion object {
        @JvmStatic
        fun testData() = TransferStatus.values()
    }
}
