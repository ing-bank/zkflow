package io.ivno.collateraltoken.zinc.types.membershipattestation

import com.ing.zkflow.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.membershipAttestation
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.membership.MembershipAttestation
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.plus

class DeserializeMembershipAttestationTest :
    DeserializationTestBase<DeserializeMembershipAttestationTest, DeserializeMembershipAttestationTest.Data>(
        { it.data.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeMembershipAttestationTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule + CordaSerializers.module

    @Serializable
    data class Data(val data: @Contextual MembershipAttestation)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(membershipAttestation),
        )
    }
}
