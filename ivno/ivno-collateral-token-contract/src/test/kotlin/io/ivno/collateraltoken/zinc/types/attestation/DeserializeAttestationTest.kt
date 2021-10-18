package io.ivno.collateraltoken.zinc.types.attestation

import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.attestation
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.identityframework.contract.Attestation
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.plus

class DeserializeAttestationTest :
    DeserializationTestBase<DeserializeAttestationTest, DeserializeAttestationTest.Data>(
        { it.data.toZincJson() }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAttestationTest>()

        override fun getSerializersModule() = IvnoSerializers.serializersModule + CordaSerializers.module

    @Serializable
    data class Data(val data: @Contextual Attestation<*>)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(attestation),
        )
    }
}
