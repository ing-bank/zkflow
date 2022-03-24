@file:Suppress("ClassName")

package io.ivno.annotated.fixtures

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.ZKPSurrogate
import io.ivno.annotated.IvnoTokenType
import io.ivno.annotated.deps.BigDecimalAmount
import net.corda.core.contracts.LinearPointer
import java.math.BigDecimal

@ZKPSurrogate(BigDecimalAmount_LinearPointer_IvnoTokenType_Converter::class)
data class BigDecimalAmount_LinearPointer_IvnoTokenType(
    val quantity: @BigDecimalSize(10, 10) BigDecimal,
    val amountType: @Via<LinearPointerSurrogate_IvnoTokenType> LinearPointer<IvnoTokenType>
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
