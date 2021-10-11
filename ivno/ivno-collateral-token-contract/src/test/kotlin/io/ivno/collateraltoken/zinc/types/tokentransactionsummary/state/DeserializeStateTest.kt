package io.ivno.collateraltoken.zinc.types.tokentransactionsummary.state

import com.ing.zkflow.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenTransactionSummary.State
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.state
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto

class DeserializeStateTest :
    DeserializationTestBase<DeserializeStateTest, DeserializeStateTest.Data>(
        {
            it.data.toZincJson(
                encodedSize =  EdDSASurrogate.ENCODED_SIZE,
                isAnonymous = false,
                scheme = Crypto.EDDSA_ED25519_SHA512
            )
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeStateTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual State)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(state),
        )
    }
}