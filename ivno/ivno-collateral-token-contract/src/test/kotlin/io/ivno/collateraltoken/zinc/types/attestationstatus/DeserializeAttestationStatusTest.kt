package io.ivno.collateraltoken.zinc.types.attestationstatus

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.identityframework.contract.AttestationStatus

class DeserializeAttestationStatusTest :
    DeserializationTestBase<DeserializeAttestationStatusTest, AttestationStatus>(
        { it.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAttestationStatusTest>()

    companion object {
        @JvmStatic
        fun testData(): Array<AttestationStatus> = AttestationStatus.values()
    }
}
