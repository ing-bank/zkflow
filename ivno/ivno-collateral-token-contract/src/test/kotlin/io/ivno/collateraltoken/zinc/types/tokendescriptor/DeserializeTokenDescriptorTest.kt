package io.ivno.collateraltoken.zinc.types.tokendescriptor

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.serialization.RoleSerializer
import io.ivno.collateraltoken.serialization.TokenDescriptorSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.identity.CordaX500Name

class DeserializeTokenDescriptorTest :
DeserializationTestBase<DeserializeTokenDescriptorTest, DeserializeTokenDescriptorTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeTokenDescriptorTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = SerializersModule { contextual(TokenDescriptorSerializer) }

    @Serializable
    data class Data(val data: @Contextual TokenDescriptor)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TokenDescriptor("Prince", CordaX500Name.parse("O=tafkap,L=New York,C=US"))),
        )
    }
}
