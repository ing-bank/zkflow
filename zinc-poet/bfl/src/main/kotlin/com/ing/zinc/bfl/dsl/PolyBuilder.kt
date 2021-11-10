package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflPoly
import com.ing.zinc.bfl.BflType

class PolyBuilder {
    var innerType: BflType? = null

    fun build() = BflPoly(
        requireNotNull(innerType) { "Poly property innerType is missing" }
    )

    companion object {
        fun poly(init: PolyBuilder.() -> Unit): BflPoly = PolyBuilder().apply(init).build()
    }
}
