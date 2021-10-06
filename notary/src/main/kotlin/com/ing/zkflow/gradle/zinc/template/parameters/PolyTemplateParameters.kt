package com.ing.zkflow.gradle.zinc.template.parameters

import com.ing.zkflow.gradle.zinc.template.TemplateParameters

data class PolyTemplateParameters<T : TemplateParameters>(val type: T) : TemplateParameters(
    "poly.zn",
    listOf(type)
) {
    override val typeName = "Poly${type.typeName}"
    override fun getModuleName() = "poly_${type.getModuleName()}"
    override fun getTargetFilename() = "${getModuleName()}.zn"
    override fun getReplacements() = getTypeReplacements() + type.getTypeReplacements("T_")
}

inline fun <reified T : TemplateParameters> T.polymorphic(): PolyTemplateParameters<T> =
    PolyTemplateParameters(this)
