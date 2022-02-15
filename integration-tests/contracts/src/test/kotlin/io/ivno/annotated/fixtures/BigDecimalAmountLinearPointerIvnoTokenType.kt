@file:Suppress("ClassName")

package io.ivno.annotated.fixtures

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Converter
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.ZKP
import io.ivno.annotated.IvnoTokenType
import io.ivno.annotated.deps.BigDecimalAmount
import net.corda.core.contracts.LinearPointer
import java.math.BigDecimal

@ZKP
data class BigDecimalAmount_LinearPointer_IvnoTokenType(
    val quantity: @BigDecimalSize(10, 10) BigDecimal,
    val amountType: @Converter<LinearPointer<IvnoTokenType>, LinearPointerSurrogate_IvnoTokenType>(
        LinearPointerConverter_IvnoTokenType::class
    ) LinearPointer<IvnoTokenType>
) : Surrogate<BigDecimalAmount<LinearPointer<IvnoTokenType>>> {
    override fun toOriginal() = BigDecimalAmount(quantity, amountType)
}

object BigDecimalAmount_LinearPointer_IvnoTokenType_Converter : ConversionProvider<
        BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        BigDecimalAmount_LinearPointer_IvnoTokenType
        > {
    override fun from(original: BigDecimalAmount<LinearPointer<IvnoTokenType>>) =
        BigDecimalAmount_LinearPointer_IvnoTokenType(original.quantity, original.amountType)
}
