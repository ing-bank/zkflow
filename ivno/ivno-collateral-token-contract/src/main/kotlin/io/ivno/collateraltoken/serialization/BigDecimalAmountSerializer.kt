package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.api.Surrogate
import io.dasl.contracts.v1.token.BigDecimalAmount
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

@Serializable
data class BigDecimalAmountSurrogate<T : Any>(
    val quantity: @Contextual BigDecimal,
    val token: @Contextual T,
) : Surrogate<BigDecimalAmount<T>> {
    override fun toOriginal(): BigDecimalAmount<T> = BigDecimalAmount(quantity, token)
}

class BigDecimalAmountSerializer<T : Any>(tokenSerializer: KSerializer<T>) : KSerializer<BigDecimalAmount<T>> {
    private val surrogateSerializer = BigDecimalAmountSurrogate.serializer(tokenSerializer)
    override fun deserialize(decoder: Decoder): BigDecimalAmount<T> {
        return decoder.decodeSerializableValue(surrogateSerializer).toOriginal()
    }

    override val descriptor: SerialDescriptor
        get() = surrogateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: BigDecimalAmount<T>) {
        encoder.encodeSerializableValue(
            surrogateSerializer,
            BigDecimalAmountSurrogate(
                value.quantity,
                value.amountType
            )
        )
    }
}
