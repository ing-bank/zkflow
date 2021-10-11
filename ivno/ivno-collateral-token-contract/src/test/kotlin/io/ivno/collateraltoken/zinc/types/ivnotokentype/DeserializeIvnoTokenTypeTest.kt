package io.ivno.collateraltoken.zinc.types.ivnotokentype

import com.ing.zkflow.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.zinc.types.ivnoTokenType
import io.ivno.collateraltoken.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto

class DeserializeIvnoTokenTypeTest :
    DeserializationTestBase<DeserializeIvnoTokenTypeTest, DeserializeIvnoTokenTypeTest.Data>(
        {
            it.data.toZincJson(
                networkEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                isAnonymous = false,
                networkScheme = Crypto.EDDSA_ED25519_SHA512,
                custodianEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                custodianScheme = Crypto.EDDSA_ED25519_SHA512,
                tokenIssuingEntityEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                tokenIssuingEntityScheme = Crypto.EDDSA_ED25519_SHA512,
            )
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeIvnoTokenTypeTest>()

    @ExperimentalSerializationApi
    override fun getSerializersModule() = IvnoSerializers.serializersModule

    @Serializable
    data class Data(val data: @Contextual IvnoTokenType)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(ivnoTokenType),
        )
    }
}