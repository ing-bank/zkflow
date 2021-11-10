package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.Field

class FieldBuilder {
    var name: String? = null
    var type: BflType? = null

    fun build() = Field(
        requireNotNull(name) { "Field property name is missing" },
        requireNotNull(type) { "Field property type is missing" }
    )

    companion object {
        fun field(init: FieldBuilder.() -> Unit): BflStructField = FieldBuilder().apply(init).build()
    }
}
