package io.ivno.collateraltoken.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * BogusSerializer is used as the default bogus inner serializer for Serializers of generic classes (e.g. BigDecimalAmount)
 * It is used so that the outer serializer can be registered as contextual, yet, the actual inner serializers are
 * registered on spot using the Serializable(with = ...) annotation
 */
object BogusSerializer : KSerializer<Int> {
    override fun serialize(encoder: Encoder, value: Int) {
        fail()
    }

    override val descriptor: SerialDescriptor
        get() = fail()

    override fun deserialize(decoder: Decoder): Int {
        fail()
    }

    private fun fail(): Nothing = error("This serializer is a placeholder and should never be called")
}