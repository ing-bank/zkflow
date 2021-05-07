package zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.SerialName
import net.corda.core.crypto.Crypto
import kotlin.reflect.full.findAnnotation

class DeserializeEdDSAPublicKeyTest :
    DeserializePublicKeyTestBase<DeserializeEdDSAPublicKeyTest>(
        scheme = Crypto.EDDSA_ED25519_SHA512,
        serialName = EdDSASurrogate::class.findAnnotation<SerialName>()!!.value,
        encodedSize = EdDSASurrogate.ENCODED_SIZE,
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeEdDSAPublicKeyTest>()
}
