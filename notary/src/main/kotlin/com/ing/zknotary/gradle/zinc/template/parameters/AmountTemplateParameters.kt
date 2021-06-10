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
    override val typeName: String by lazy { typeNameOverride ?: AMOUNT + postFix() + tokenDisplaySize.typeName }

    override fun getReplacements() = getTypeReplacements() +
        tokenDisplaySize.getTypeReplacements("BD_") +
        mapOf(
            "TOKEN_SIZE_PLACEHOLDER" to tokenSize.toString(),
        )

    override fun getTargetFilename() = getFileName()

    companion object {
        const val AMOUNT = "Amount"
    }
}
