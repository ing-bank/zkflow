package com.ing.zknotary.common.serialization.bfl.corda

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import java.math.BigDecimal

fun Amount<*>.getTokenTypeHash(): ByteArray = SecureHash.sha256(token.javaClass.name).copyBytes()

@Serializable
@Suppress("ArrayInDataClass")
data class AmountSurrogate<T : Any>(
    val quantity: Long,
    @FixedLength([DISPLAY_TOKEN_SIZE_INTEGER_LENGTH, DISPLAY_TOKEN_SIZE_FRACTION_LENGTH])
    val displayTokenSize: @Contextual BigDecimal,
    @FixedLength([TOKEN_TYPE_HASH_LENGTH])
    val tokenTypeHash: ByteArray,
    val token: @Contextual T,
) : Surrogate<Amount<T>> {
    override fun toOriginal(): Amount<T> = Amount(quantity, displayTokenSize, token)

    companion object {
        const val TOKEN_TYPE_HASH_LENGTH = 32
        const val DISPLAY_TOKEN_SIZE_INTEGER_LENGTH = 100
        const val DISPLAY_TOKEN_SIZE_FRACTION_LENGTH = 20
    }
}

class AmountSerializer<T : Any>(tokenSerializer: KSerializer<T>) :
    SurrogateSerializer<Amount<T>, AmountSurrogate<T>>(
        AmountSurrogate.serializer(tokenSerializer),
        { AmountSurrogate(it.quantity, it.displayTokenSize, it.getTokenTypeHash(), it.token) }
    )
