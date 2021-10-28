package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters

data class MapTemplateParameters(
    override val typeName: String,
    val capacity: Int,
    val keyTemplateParameters: TemplateParameters,
    val valueTemplateParameters: TemplateParameters,
) : TemplateParameters(
    "map.zn",
    listOf(
        keyTemplateParameters,
        valueTemplateParameters
    )
) {
    override fun getTargetFilename() = getFileName()

    override fun getReplacements() = getTypeReplacements() +
        keyTemplateParameters.getTypeReplacements("KEY_") +
        valueTemplateParameters.getTypeReplacements("VALUE_") +
        mapOf("CAPACITY" to "$capacity")
}