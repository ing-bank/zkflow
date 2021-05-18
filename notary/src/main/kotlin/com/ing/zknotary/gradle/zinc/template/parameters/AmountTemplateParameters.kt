package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class AmountTemplateParameters(
    val tokenDisplaySize: BigDecimalTemplateParameters,
    val tokenSize: Short,
    val typeNameOverride: String? = null
) : TemplateParameters("amount.zn", listOf(tokenDisplaySize)) {
    private fun postFix(): String = "_${tokenSize}_"

    /**
     * The name of the struct for this configuration.
     */
    private val typeName: String by lazy { typeNameOverride ?: AMOUNT + postFix() + tokenDisplaySize.typeName }

    override fun getReplacements() = mapOf(
        "TYPE_NAME" to typeName,
        "CONSTANT_PREFIX" to typeName.camelToSnakeCase().toUpperCase(),
        "TOKEN_SIZE_PLACEHOLDER" to tokenSize.toString(),
        "BD_TYPE_NAME" to tokenDisplaySize.typeName,
        "BD_CONSTANT_PREFIX" to tokenDisplaySize.typeName.camelToSnakeCase().toUpperCase(),
        "BD_MODULE_NAME" to tokenDisplaySize.typeName.camelToSnakeCase(),
    )

    override fun getTargetFilename() = "${typeName.camelToSnakeCase()}.zn"

    companion object {
        const val AMOUNT = "Amount"
    }
}
