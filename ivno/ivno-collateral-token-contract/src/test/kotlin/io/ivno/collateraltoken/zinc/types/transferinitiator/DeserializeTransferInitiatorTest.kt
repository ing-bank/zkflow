package io.ivno.collateraltoken.zinc.types.transferinitiator

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
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
