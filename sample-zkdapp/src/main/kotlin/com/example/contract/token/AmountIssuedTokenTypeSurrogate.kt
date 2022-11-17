package com.example.contract.token

import com.example.token.sdk.IssuedTokenType
import com.example.token.sdk.TokenType
import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.annotations.corda.EdDSA
import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import java.math.BigDecimal

@ZKPSurrogate(AmountIssuedTokenTypeConverter::class)
data class AmountIssuedTokenTypeSurrogate(
    val quantity: Long,
    val displayTokenSize: @BigDecimalSize(INT_PRECISION, FRAC_PRECISION) BigDecimal,
    val issuer: @EdDSA Party,
    val tokenIdentifier: @UTF8(TOKEN_IDENTIFIER_LENGTH) String,
    val fractionDigits: Int,
) : Surrogate<Amount<IssuedTokenType>> {
    companion object {
        const val INT_PRECISION: Int = 10
        const val FRAC_PRECISION: Int = 10
        const val TOKEN_IDENTIFIER_LENGTH: Int = 10
    }

    override fun toOriginal(): Amount<IssuedTokenType> = Amount(
        quantity,
        displayTokenSize,
        IssuedTokenType(issuer, TokenType(tokenIdentifier, fractionDigits))
    )
}

object AmountIssuedTokenTypeConverter : ConversionProvider<Amount<IssuedTokenType>, AmountIssuedTokenTypeSurrogate> {
    override fun from(original: Amount<IssuedTokenType>) =
        AmountIssuedTokenTypeSurrogate(
            original.quantity,
            original.displayTokenSize,
            original.token.issuer,
            original.token.tokenType.tokenIdentifier,
            original.token.tokenType.fractionDigits
        )
}
