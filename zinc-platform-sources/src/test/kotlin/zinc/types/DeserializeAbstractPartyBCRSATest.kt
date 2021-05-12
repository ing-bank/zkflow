package zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
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
class DeserializeAbstractPartyBCRSATest :
    DeserializationTestBase<DeserializeAbstractPartyBCRSATest, DeserializeAbstractPartyBCRSATest.Data>({
        it.data.toZincJson(
            serialName = BCRSASurrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCRSASurrogate.ENCODED_SIZE,
        )
    }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAbstractPartyBCRSATest>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.RSA_SHA256)
    }

    @Serializable
    data class Data(val data: @Contextual AbstractParty)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.RSA_SHA256).party),
            Data(TestIdentity.fresh("Alice", Crypto.RSA_SHA256).party.anonymise()),
        )
    }
}
