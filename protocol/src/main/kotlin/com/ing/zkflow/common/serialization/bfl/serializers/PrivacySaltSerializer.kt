package com.ing.zkflow.common.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.PrivacySalt

object PrivacySaltSerializer : SurrogateSerializer<PrivacySalt, PrivacySaltSurrogate>(
    PrivacySaltSurrogate.serializer(),
    { PrivacySaltSurrogate(it.bytes) }
)

@Suppress("ArrayInDataClass")
@Serializable
data class PrivacySaltSurrogate(
    // Hashes expected by Corda must be at most 32 bytes long.
    @FixedLength([32])
    val bytes: ByteArray
) : Surrogate<PrivacySalt> {
    override fun toOriginal(): PrivacySalt = PrivacySalt(bytes)
}
