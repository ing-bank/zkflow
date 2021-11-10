package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflOption
import com.ing.zinc.bfl.BflType

class OptionBuilder {
    var innerType: BflType? = null

    fun build() = BflOption(
        requireNotNull(innerType) { "Option property innerType is missing" }
    )

    companion object {
        fun option(init: OptionBuilder.() -> Unit): BflOption = OptionBuilder().apply(init).build()
    }
}
