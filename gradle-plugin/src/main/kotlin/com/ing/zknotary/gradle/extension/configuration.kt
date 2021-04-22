package com.ing.zknotary.gradle.extension

data class BigDecimalTemplateParameters(
    val integerSize: Short,
    val fractionSize: Short,
    val typeNameOverride: String? = null
) {
    private fun postFix(): String = "_${integerSize}_$fractionSize"

    /**
     * The name of the struct for this configuration.
     */
    val typeName: String by lazy { typeNameOverride ?: BIG_DECIMAL + postFix() }

    companion object {
        const val BIG_DECIMAL = "BigDecimal"
    }
}

data class AmountTemplateParameters(
    val tokenDisplaySize: BigDecimalTemplateParameters,
    val tokenSize: Short,
    val typeNameOverride: String? = null
) {
    private fun postFix(): String = "_${tokenSize}_"

    /**
     * The name of the struct for this configuration.
     */
    val typeName: String by lazy { typeNameOverride ?: AMOUNT + postFix() + tokenDisplaySize.typeName }

    companion object {
        const val AMOUNT = "Amount"
    }
}

private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()

internal fun String.camelToSnakeCase(): String {
    return camelRegex.replace(this) {
        "_${it.value}"
    }.toLowerCase()
}
