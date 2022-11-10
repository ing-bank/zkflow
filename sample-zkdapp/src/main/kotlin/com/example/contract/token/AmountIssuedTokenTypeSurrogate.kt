package com.example.contract.token

import com.example.token.sdk.IssuedTokenType
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
    // TODO: flatten this to get rid of the need to annotate IssuedTokenType (which we couldn't do, because it is third-party)
    // Also use it as an example to explain the need to flatten it, in relation to versioning.
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
