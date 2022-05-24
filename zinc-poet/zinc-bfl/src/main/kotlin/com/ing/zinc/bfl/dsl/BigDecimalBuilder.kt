package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflBigDecimal

class BigDecimalBuilder {
    var integerSize: Int? = null
    var fractionSize: Int? = null
    var name: String? = null

    fun build() = BflBigDecimal(
        requireNotNull(integerSize) { "BigDecimal property integerSize is missing" },
        requireNotNull(fractionSize) { "BigDecimal property fractionSize is missing" },
        name
    )

    companion object {
        fun bigDecimal(init: BigDecimalBuilder.() -> Unit): BflBigDecimal = BigDecimalBuilder().apply(init).build()
    }
}
