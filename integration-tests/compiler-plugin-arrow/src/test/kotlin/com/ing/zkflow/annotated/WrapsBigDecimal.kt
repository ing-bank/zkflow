package com.ing.zkflow.annotated

import com.ing.zkflow.BigDecimalSize
import com.ing.zkflow.ZKP
import java.math.BigDecimal

@ZKP
data class WrapsBigDecimal(
    val bigDecimal: @BigDecimalSize(5, 5) BigDecimal = 12.34.toBigDecimal()
)
