package com.ing.zknotary.common.serialization.bfl.serializers.publickey

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PublicKeySurrogate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.bouncycastle.pqc.jcajce.provider.sphincs.BCSphincs256PublicKey

object BCSphincs256PublicKeySerializer : KSerializer<BCSphincs256PublicKey> by (
    SurrogateSerializer(BCSphincs256Surrogate.serializer()) {
        BCSphincs256Surrogate(it.encoded)
    }
    )

@Suppress("ArrayInDataClass")
@Serializable
/**
 * Corresponds to Corda's Crypto.256_SHA512 (1097 bytes).
 */
data class BCSphincs256Surrogate(@FixedLength([1097]) override val encoded: ByteArray) : PublicKeySurrogate<BCSphincs256PublicKey> {
    override fun toOriginal() = toOriginal(Crypto.SPHINCS256_SHA256.schemeNumberID) as BCSphincs256PublicKey
}
