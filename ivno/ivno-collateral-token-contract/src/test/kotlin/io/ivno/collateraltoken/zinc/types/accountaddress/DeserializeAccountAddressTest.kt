package io.ivno.collateraltoken.zinc.types.accountaddress

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.account.AccountAddress
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name

class DeserializeAccountAddressTest :
DeserializationTestBase<DeserializeAccountAddressTest, DeserializeAccountAddressTest.Data>(
    { it.data.toZincJson() }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAccountAddressTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual AccountAddress)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(AccountAddress("Prince", CordaX500Name.parse("O=tafkap,L=New York,C=US"))),
        )
    }
}