package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class PolyTemplateParameters<T : TemplateParameters>(val type: T) : TemplateParameters(
    "poly.zn",
    listOf(type)
) {
    override val typeName = "Poly${type.typeName}"
    override fun getModuleName() = "poly_${type.getModuleName()}"
    override fun getTargetFilename() = "${getModuleName()}.zn"
    override fun getReplacements() = getTypeReplacements() + type.getTypeReplacements("T_")
}
