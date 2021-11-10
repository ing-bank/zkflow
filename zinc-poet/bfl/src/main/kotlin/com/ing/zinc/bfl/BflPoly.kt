package com.ing.zinc.bfl

import com.ing.zinc.bfl.dsl.ListBuilder.Companion.utfString

data class BflPoly(
    val innerType: BflType
) : BflStruct(
    "${innerType.typeName()}Poly",
    listOf(
        Field(SERIAL_NAME_FIELD, utfString(1)),
        Field(INNER_FIELD, innerType)
    )
) {
    companion object {
        const val SERIAL_NAME_FIELD = "serial_name"
        const val INNER_FIELD = "inner"
    }
}
