package zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import java.security.PublicKey

abstract class DeserializePublicKeyTestBase<T : DeserializePublicKeyTestBase<T>>(
    private val scheme: SignatureScheme,
    private val serialName: String,
    private val encodedSize: Int
) : DeserializationTestBase<T, DeserializePublicKeyTestBase.Data>(
    { it.data.toZincJson(serialName, encodedSize) },
) {
    override fun getSerializersModule() = CordaSignatureSchemeToSerializers.serializersModuleFor(scheme)

    @Serializable
    data class Data(
        val data: @Polymorphic PublicKey
    )

    fun testData() = listOf(
        Data(Crypto.generateKeyPair(scheme).public)
    )
}
