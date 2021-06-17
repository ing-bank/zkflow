package io.ivno.collateraltoken.zinc.types.transferinitiator

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import io.ivno.collateraltoken.contract.TransferInitiator

class DeserializeTransferInitiatorTest :
    DeserializationTestBase<DeserializeTransferInitiatorTest, TransferInitiator>(
        { it.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeTransferInitiatorTest>()

    companion object {
        @JvmStatic
        fun testData() = TransferInitiator.values()
    }
}
