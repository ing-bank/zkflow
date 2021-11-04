package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.BflType

class FieldBuilder {
    var name: String? = null
    var type: BflType? = null

    fun build() = BflStruct.Field(
        requireNotNull(name) { "Field property name is missing" },
        requireNotNull(type) { "Field property type is missing" }
    )

    companion object {
        fun field(init: FieldBuilder.() -> Unit): BflStruct.Field = FieldBuilder().apply(init).build()
    }
}
