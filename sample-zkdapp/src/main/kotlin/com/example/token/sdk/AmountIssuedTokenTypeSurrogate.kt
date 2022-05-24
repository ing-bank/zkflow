package com.example.token.sdk

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.ZKPSurrogate
import net.corda.core.contracts.Amount
import java.math.BigDecimal

@ZKPSurrogate(AmountIssuedTokenTypeConverter::class)
data class AmountIssuedTokenTypeSurrogate(
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

object AmountIssuedTokenTypeConverter : ConversionProvider<Amount<IssuedTokenType>, AmountIssuedTokenTypeSurrogate> {
    override fun from(original: Amount<IssuedTokenType>) =
        AmountIssuedTokenTypeSurrogate(original.quantity, original.displayTokenSize, original.token)
}
