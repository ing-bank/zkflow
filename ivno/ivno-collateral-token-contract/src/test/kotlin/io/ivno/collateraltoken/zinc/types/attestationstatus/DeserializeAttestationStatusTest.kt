package io.ivno.collateraltoken.zinc.types.attestationstatus

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import io.onixlabs.corda.identityframework.contract.AttestationStatus

class DeserializeAttestationStatusTest : DeserializationTestBase<DeserializeAttestationStatusTest, AttestationStatus>(
    { it.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAttestationStatusTest>()

    companion object {
        @JvmStatic
        fun testData() = AttestationStatus.values()
    }
}
