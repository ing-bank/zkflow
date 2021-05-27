package io.ivno.collateraltoken.serialization

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import io.dasl.contracts.v1.token.BigDecimalAmount
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class BigDecimalAmountSurrogate<T : Any>(
    val quantity: @Contextual BigDecimal,
    val token: @Contextual T,
) : Surrogate<BigDecimalAmount<T>> {
    override fun toOriginal(): BigDecimalAmount<T> = BigDecimalAmount(quantity, token)
}

class BigDecimalAmountSerializer<T : Any>(tokenSerializer: KSerializer<T>) :
    SurrogateSerializer<BigDecimalAmount<T>, BigDecimalAmountSurrogate<T>>(
        BigDecimalAmountSurrogate.serializer(tokenSerializer),
        { BigDecimalAmountSurrogate(it.quantity, it.amountType) }
    )