package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters

data class ByteArrayTemplateParameters(
    val arraySize: Int
) : TemplateParameters("byte_array.zn", emptyList()) {
    override val typeName = "ByteArray_$arraySize"
    override fun getReplacements() = mapOf(
        "ARRAY_LENGTH" to "$arraySize",
    )

    override fun getTargetFilename() = getFileName()
}
