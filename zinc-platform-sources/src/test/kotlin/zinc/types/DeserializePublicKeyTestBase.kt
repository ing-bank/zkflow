package zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import java.security.PublicKey

abstract class DeserializePublicKeyTestBase<T : DeserializePublicKeyTestBase<T>>(
    private val scheme: SignatureScheme,
    private val serialName: String,
    private val encodedSize: Int
) : DeserializationTestBase<T, DeserializePublicKeyTestBase.Data>(
    { it.toZincJson(scheme, serialName, encodedSize) },
) {
    override fun getSerializersModule() = CordaSignatureSchemeToSerializers.serializersModuleFor(scheme)

    @Serializable
    data class Data(
        val data: @Polymorphic PublicKey
    ) {
        fun toZincJson(scheme: SignatureScheme, serialName: String, encodedSize: Int) =
            buildJsonObject {
                put("serial_name", serialName.toJsonObject(1))

                if (scheme == Crypto.ECDSA_SECP256K1_SHA256 || scheme == Crypto.ECDSA_SECP256R1_SHA256) {
                    put("scheme_id", "${scheme.schemeNumberID}")
                }

                put("encoded", data.encoded.toJsonObject(encodedSize))
            }.toString()
    }

    fun testData() = listOf(
        Data(Crypto.generateKeyPair(scheme).public)
    )
}
