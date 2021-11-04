package com.ing.zinc.bfl

import com.ing.zinc.bfl.dsl.ListBuilder.Companion.utfString

data class BflPoly(
    val innerType: BflType
) : BflStruct(
    "${innerType.typeName()}Poly",
    listOf(
        Field(serialNameFieldName, utfString(1)),
        Field(innerFieldName, innerType)
    )
) {
    companion object {
        const val serialNameFieldName = "serial_name"
        const val innerFieldName = "inner"
    }
}
