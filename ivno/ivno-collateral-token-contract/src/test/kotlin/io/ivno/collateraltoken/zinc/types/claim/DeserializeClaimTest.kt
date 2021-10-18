package io.ivno.collateraltoken.zinc.types.claim

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.ClaimSerializer
import io.ivno.collateraltoken.serialization.ClaimSurrogate
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class DeserializeClaimTest :
    DeserializationTestBase<DeserializeClaimTest, DeserializeClaimTest.Data>(
        {
            it.data.toZincJson(ClaimSurrogate.PROPERTY_LENGTH, VALUE_LENGTH)
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeClaimTest>()

        override fun getSerializersModule() = SerializersModule {
        polymorphic(AbstractClaim::class) {
            subclass(ClaimSerializer(String.serializer()))
        }
    }

    @Serializable
    data class Data(@FixedLength([VALUE_LENGTH]) val data: @Polymorphic AbstractClaim<String>)

    companion object {
        const val VALUE_LENGTH = 7

        @JvmStatic
        fun testData() = listOf(Data(Claim("Property", "Value")))
    }
}