package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.api.Surrogate
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference

@Serializable
data class IssuedSurrogate<T : Any>(
    val issuer: @Contextual PartyAndReference,
    val product: @Contextual T
) : Surrogate<Issued<T>> {
    override fun toOriginal(): Issued<T> = Issued(issuer, product)
}

class IssuedSerializer<T : Any>(productSerializer: KSerializer<T>) : KSerializer<Issued<T>> {
    private val surrogateSerializer = IssuedSurrogate.serializer(productSerializer)
    override fun deserialize(decoder: Decoder): Issued<T> {
        return decoder.decodeSerializableValue(surrogateSerializer).toOriginal()
    }

    override val descriptor: SerialDescriptor
        get() = surrogateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Issued<T>) {
        encoder.encodeSerializableValue(
            surrogateSerializer,
            IssuedSurrogate(value.issuer, value.product)
        )
    }
}
