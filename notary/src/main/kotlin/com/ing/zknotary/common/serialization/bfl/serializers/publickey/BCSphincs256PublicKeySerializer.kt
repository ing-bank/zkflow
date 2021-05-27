package com.ing.zknotary.common.serialization.bfl.serializers.publickey

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PublicKeySurrogate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.crypto.Crypto
import org.bouncycastle.pqc.jcajce.provider.sphincs.BCSphincs256PublicKey

object BCSphincs256PublicKeySerializer :
    SurrogateSerializer<BCSphincs256PublicKey, BCSphincs256Surrogate>(
        BCSphincs256Surrogate.serializer(),
        { BCSphincs256Surrogate(it.encoded) }
    )

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("c")
/**
 * Corresponds to Corda's Crypto.256_SHA512 (1097 bytes).
 */
data class BCSphincs256Surrogate(@FixedLength([ENCODED_SIZE]) override val encoded: ByteArray) : PublicKeySurrogate<BCSphincs256PublicKey> {
    override fun toOriginal() = toOriginal(Crypto.SPHINCS256_SHA256.schemeNumberID) as BCSphincs256PublicKey

    companion object {
        const val ENCODED_SIZE = 1097
    }
}
