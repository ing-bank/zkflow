package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class StringTemplateParameters(
    val stringSize: Short
) : TemplateParameters("string.zn", emptyList()) {
    override fun getReplacements() = mapOf(
        "TYPE_NAME" to "String_$stringSize",
        "CONSTANT_PREFIX" to "STRING_$stringSize",
        "STRING_SIZE_PLACEHOLDER" to stringSize.toString(),
    )

    override fun getTargetFilename() = "string_$stringSize.zn"
}
