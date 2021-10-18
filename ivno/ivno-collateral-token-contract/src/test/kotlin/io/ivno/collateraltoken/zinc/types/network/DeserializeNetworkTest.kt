package io.ivno.collateraltoken.zinc.types.network

import com.ing.zkflow.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.network
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto

class DeserializeNetworkTest :
    DeserializationTestBase<DeserializeNetworkTest, DeserializeNetworkTest.Data>(
        {
            it.data.toZincJson(
                encodedSize =  EdDSASurrogate.ENCODED_SIZE,
                isAnonymous = false,
                scheme = Crypto.EDDSA_ED25519_SHA512
            )
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeNetworkTest>()

        override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual Network)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(network),
        )
    }
}