package io.ivno.collateraltoken.zinc.types.ivnotokentype

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.serialization.IvnoSerializers
import io.ivno.collateraltoken.serialization.IvnoTokenTypeSerializer
import io.ivno.collateraltoken.zinc.types.toZincJson
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Crypto
import net.corda.testing.core.TestIdentity
import java.util.*

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
        private val alice = TestIdentity.fresh("Alice").party
        private val bob = TestIdentity.fresh("Bob").party

        @JvmStatic
        fun testData() = listOf(
            Data(
                IvnoTokenType(
                    Network("Network", alice),
                    bob,
                    alice,
                    "Display Name",
                    1,
                    UniqueIdentifier(id = UUID(0, 1))
                )
            ),
        )
    }
}