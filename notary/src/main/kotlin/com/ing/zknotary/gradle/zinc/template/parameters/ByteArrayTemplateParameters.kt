package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.NamedType
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class ByteArrayTemplateParameters(
    val arraySize: Int
) : NamedType, TemplateParameters("byte_array.zn", emptyList()) {
    override val typeName = "ByteArray_$arraySize"
    override fun getReplacements() = mapOf(
        "ARRAY_LENGTH" to "$arraySize",
    )

    override fun getTargetFilename() = getFileName()
}
