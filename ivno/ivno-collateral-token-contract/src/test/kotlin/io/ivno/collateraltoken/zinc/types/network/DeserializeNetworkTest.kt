package io.ivno.collateraltoken.zinc.types.network

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.testing.core.TestIdentity

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

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual Network)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(
                Network(
                    value = "Network 1",
                    operator = TestIdentity.fresh("Alice").party
                )
            ),
        )
    }
}