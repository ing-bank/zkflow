package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.NamedType
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class BigDecimalTemplateParameters(
    val integerSize: Short,
    val fractionSize: Short,
    val typeNameOverride: String? = null
) : NamedType, TemplateParameters("big_decimal.zn", emptyList()) {
    private fun postFix(): String = "_${integerSize}_$fractionSize"

    /**
     * The name of the struct for this configuration.
     */
    override val typeName: String by lazy { typeNameOverride ?: BIG_DECIMAL + postFix() }

    override fun getReplacements(): Map<String, String> = getTypeReplacements() + mapOf(
        "INTEGER_SIZE_PLACEHOLDER" to integerSize.toString(),
        "FRACTION_SIZE_PLACEHOLDER" to fractionSize.toString(),
    )

    override fun getTargetFilename() = getFileName()

    companion object {
        const val BIG_DECIMAL = "BigDecimal"
    }
}
