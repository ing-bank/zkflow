package com.ing.zinc.bfl

import com.ing.zinc.naming.camelToSnakeCase
import java.util.Locale

interface BflStructField {
    val name: String
    val type: BflType
}

data class Field(
    override val name: String,
    override val type: BflType,
) : BflStructField

data class FieldWithParentStruct(
    override val name: String,
    override val type: BflType,
    val struct: BflStruct,
) : BflStructField {
    /**
     * Generate a constant for this field with the given [suffix].
     */
    fun generateConstant(suffix: String): String {
        val typeName = struct.id.camelToSnakeCase()
        return "${typeName}_${name}_$suffix".toUpperCase(Locale.getDefault())
    }
}
