package zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.testing.core.TestIdentity
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializeAnonymousPartyBCSphincs256Test :
    DeserializationTestBase<DeserializeAnonymousPartyBCSphincs256Test, DeserializeAnonymousPartyBCSphincs256Test.Data>({
        it.data.toZincJson(
            serialName = BCSphincs256Surrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCSphincs256Surrogate.ENCODED_SIZE,
        )
    }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAnonymousPartyBCSphincs256Test>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.SPHINCS256_SHA256)
    }

    @Serializable
    data class Data(val data: @Contextual AnonymousParty)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.SPHINCS256_SHA256).party.anonymise()),
        )
    }
}
