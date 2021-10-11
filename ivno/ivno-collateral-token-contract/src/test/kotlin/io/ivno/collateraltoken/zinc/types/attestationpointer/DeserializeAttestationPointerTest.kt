package io.ivno.collateraltoken.zinc.types.attestationpointer

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.attestationPointer
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.identityframework.contract.AttestationPointer
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

class DeserializeAttestationPointerTest :
    DeserializationTestBase<DeserializeAttestationPointerTest, DeserializeAttestationPointerTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAttestationPointerTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual AttestationPointer<*>)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(attestationPointer),
        )
    }
}