package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class ByteArrayTemplateParameters(
    val arraySize: Int
) : TemplateParameters("byte_array.zn", emptyList()) {
    override fun getReplacements() = mapOf(
        "ARRAY_LENGTH" to "$arraySize",
    )

    override fun getTargetFilename() = "byte_array_$arraySize.zn"
}
