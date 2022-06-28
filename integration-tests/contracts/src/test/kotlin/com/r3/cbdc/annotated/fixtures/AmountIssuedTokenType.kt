@file:Suppress("ClassName")

package com.r3.cbdc.annotated.fixtures

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.ZKPSurrogate
import com.r3.cbdc.annotated.types.IssuedTokenType
import net.corda.core.contracts.Amount
import java.math.BigDecimal

@ZKPSurrogate(AmountConverter_IssuedTokenType::class)
data class AmountSurrogate_IssuedTokenTypeV1(
    val quantity: Long,
    val displayTokenSize: @BigDecimalSize(10, 10) BigDecimal,
    val token: IssuedTokenType
) : Surrogate<Amount<IssuedTokenType>> {
    override fun toOriginal(): Amount<IssuedTokenType> = Amount(quantity, displayTokenSize, token)
}

object AmountConverter_IssuedTokenType : ConversionProvider<
        Amount<IssuedTokenType>,
        AmountSurrogate_IssuedTokenTypeV1
        > {
    override fun from(original: Amount<IssuedTokenType>) =
        AmountSurrogate_IssuedTokenTypeV1(original.quantity, original.displayTokenSize, original.token)
}
