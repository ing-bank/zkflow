package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class BigDecimalTemplateParameters(
    val integerSize: Short,
    val fractionSize: Short,
    val typeNameOverride: String? = null
) : TemplateParameters("big_decimal.zn", emptyList()) {
    private fun postFix(): String = "_${integerSize}_$fractionSize"

    /**
     * The name of the struct for this configuration.
     */
    internal val typeName: String by lazy { typeNameOverride ?: BIG_DECIMAL + postFix() }

    override fun getReplacements(): Map<String, String> = mapOf(
        "TYPE_NAME" to typeName,
        "CONSTANT_PREFIX" to typeName.camelToSnakeCase().toUpperCase(),
        "INTEGER_SIZE_PLACEHOLDER" to integerSize.toString(),
        "FRACTION_SIZE_PLACEHOLDER" to fractionSize.toString(),
    )

    override fun getTargetFilename() = "${typeName.camelToSnakeCase()}.zn"

    companion object {
        const val BIG_DECIMAL = "BigDecimal"
    }
}
