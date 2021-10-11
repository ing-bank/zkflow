package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters

data class StringTemplateParameters(
    val stringSize: Int
) : TemplateParameters("string.zn", emptyList()) {
    override val typeName = "String_$stringSize"

    override fun getReplacements() = getTypeReplacements() + mapOf(
        "STRING_SIZE_PLACEHOLDER" to stringSize.toString(),
    )

    override fun getTargetFilename() = getFileName()
}
