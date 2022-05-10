package com.example.token.cbdc

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.ZKPSurrogate
import net.corda.core.contracts.Amount
import java.math.BigDecimal

@Suppress("ClassName")
@ZKPSurrogate(AmountConverter_IssuedTokenType::class)
data class AmountSurrogate_IssuedTokenType(
    val quantity: Long,
    val displayTokenSize: @BigDecimalSize(INT_PRECISION, FRAC_PRECISION) BigDecimal,
    val token: IssuedTokenType
) : Surrogate<Amount<IssuedTokenType>> {
    companion object {
        const val INT_PRECISION: Int = 10
        const val FRAC_PRECISION: Int = 10
    }

    override fun toOriginal(): Amount<IssuedTokenType> = Amount(quantity, displayTokenSize, token)
}

@Suppress("ClassName")
object AmountConverter_IssuedTokenType : ConversionProvider<
    Amount<IssuedTokenType>,
    AmountSurrogate_IssuedTokenType
    > {
    override fun from(original: Amount<IssuedTokenType>) =
        AmountSurrogate_IssuedTokenType(original.quantity, original.displayTokenSize, original.token)
}
