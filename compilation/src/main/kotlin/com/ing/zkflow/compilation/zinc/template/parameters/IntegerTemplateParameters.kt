package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters

data class IntegerTemplateParameters(
    val integerZincType: String
) : TemplateParameters(
    "integer.zn",
    emptyList()
) {
    override val typeName = integerZincType.toUpperCase()

    override fun getModuleName() = "integer_$integerZincType"

    override fun getTargetFilename() = getFileName()

    override fun getReplacements() =
        getTypeReplacements() +
            mapOf(
                "INTEGER_TYPE_NAME" to integerZincType,
                "INTEGER_CONSTANT_PREFIX" to typeName.toUpperCase()
            )

    companion object {
        val u16 = IntegerTemplateParameters("u16")
        val i16 = IntegerTemplateParameters("i16")
        val u32 = IntegerTemplateParameters("u32")
        val i32 = IntegerTemplateParameters("i32")
        val u64 = IntegerTemplateParameters("u64")
        val i64 = IntegerTemplateParameters("i64")
        val u128 = IntegerTemplateParameters("u128")
        val i128 = IntegerTemplateParameters("i128")
    }
}
