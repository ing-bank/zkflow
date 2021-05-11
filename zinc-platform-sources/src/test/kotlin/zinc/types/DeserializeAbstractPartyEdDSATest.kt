package zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.testing.core.TestIdentity
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeAbstractPartyEdDSATest :
    DeserializationTestBase<DeserializeAbstractPartyEdDSATest, DeserializeAbstractPartyEdDSATest.Data>({
        it.data.toZincJson(
            scheme = Crypto.EDDSA_ED25519_SHA512,
            serialName = EdDSASurrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = EdDSASurrogate.ENCODED_SIZE
        )
    }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAbstractPartyEdDSATest>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.EDDSA_ED25519_SHA512)
    }

    @Serializable
    data class Data(val data: @Contextual AbstractParty)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.EDDSA_ED25519_SHA512).party),
        )
    }
}
