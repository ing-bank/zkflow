package com.ing.zknotary.gradle.template

import com.ing.serialization.bfl.serializers.CurrencySurrogate
import com.ing.serialization.bfl.serializers.X500PrincipalSurrogate
import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate

sealed class TemplateParameters(
    val templateFile: String,
    private val dependencies: List<TemplateParameters>
) {
    fun resolveAllConfigurations(): List<TemplateParameters> =
        dependencies.flatMap { it.resolveAllConfigurations() } + this
}

data class BigDecimalTemplateParameters(
    val integerSize: Short,
    val fractionSize: Short,
    val typeNameOverride: String? = null
) : TemplateParameters("big_decimal.zn", emptyList()) {
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
) : TemplateParameters("amount.zn", listOf(tokenDisplaySize)) {
    private fun postFix(): String = "_${tokenSize}_"

    /**
     * The name of the struct for this configuration.
     */
    val typeName: String by lazy { typeNameOverride ?: AMOUNT + postFix() + tokenDisplaySize.typeName }

    companion object {
        const val AMOUNT = "Amount"
    }
}

data class StringTemplateParameters(
    val stringSize: Short
) : TemplateParameters("string.zn", emptyList())

data class ByteArrayTemplateParameters(
    val arraySize: Int
) : TemplateParameters("byte_array.zn", emptyList())

object UniqueIdentifierTemplateParameters : TemplateParameters(
    "unique_identifier.zn",
    listOf(StringTemplateParameters(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH.toShort()))
)

object LinearPointerTemplateParameters : TemplateParameters(
    "linear_pointer.zn",
    listOf(StringTemplateParameters(LinearPointerSurrogate.MAX_CLASS_NAME_SIZE.toShort()))
)

object X500PrincipalTemplateParameters : TemplateParameters(
    "x500_principal.zn",
    listOf(StringTemplateParameters(X500PrincipalSurrogate.PRINCIPAL_SIZE.toShort()))
)

object CurrencyTemplateParameters : TemplateParameters(
    "currency.zn",
    listOf(StringTemplateParameters(CurrencySurrogate.CURRENCY_SIZE.toShort()))
)

object SecureHashTemplateParameters : TemplateParameters(
    "secure_hash.zn",
    listOf(ByteArrayTemplateParameters(SecureHashSurrogate.BYTES_SIZE))
)

private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()

internal fun String.camelToSnakeCase(): String {
    return camelRegex.replace(this) {
        "_${it.value}"
    }.toLowerCase()
}
