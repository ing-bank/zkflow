package zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import net.corda.core.crypto.Crypto
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class DeserializePartyBCSphincs256Test :
    DeserializationTestBase<DeserializePartyBCSphincs256Test, DeserializePartyBCSphincs256Test.Data>({
        it.data.toZincJson(
            scheme = Crypto.SPHINCS256_SHA256,
            serialName = BCSphincs256Surrogate::class.findAnnotation<SerialName>()!!.value,
            encodedSize = BCSphincs256Surrogate.ENCODED_SIZE,
        )
    }) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializePartyBCSphincs256Test>()
    override fun getSerializersModule(): SerializersModule {
        return CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.SPHINCS256_SHA256)
    }

    @Serializable
    data class Data(val data: @Contextual Party)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(TestIdentity.fresh("Alice", Crypto.SPHINCS256_SHA256).party),
        )
    }
}
