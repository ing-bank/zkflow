package com.ing.zinc.bfl

import com.ing.zinc.bfl.dsl.ListBuilder.Companion.utf8String

data class BflPoly(
    val innerType: BflType
) : BflStruct(
    "${innerType.typeName()}Poly",
    listOf(
        Field(SERIAL_NAME_FIELD, utf8String(1)),
        Field(INNER_FIELD, innerType)
    )
) {
    companion object {
        const val SERIAL_NAME_FIELD = "serial_name"
        const val INNER_FIELD = "inner"
    }
}
