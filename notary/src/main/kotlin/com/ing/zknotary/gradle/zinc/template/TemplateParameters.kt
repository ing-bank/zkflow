package com.ing.zknotary.gradle.zinc.template

import com.ing.serialization.bfl.serializers.CurrencySurrogate
import com.ing.serialization.bfl.serializers.X500PrincipalSurrogate
import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate

sealed class TemplateParameters(
    open val templateFile: String,
    private val dependencies: List<TemplateParameters>
) {
    fun resolveAllConfigurations(): List<TemplateParameters> =
        dependencies.flatMap { it.resolveAllConfigurations() } + this

    open fun getReplacements(): Map<String, String> = emptyMap()
    open fun getTargetFilename(): String = templateFile
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

data class StringTemplateParameters(
    val stringSize: Short
) : TemplateParameters("string.zn", emptyList()) {
    override fun getReplacements() = mapOf(
        "TYPE_NAME" to "String_$stringSize",
        "CONSTANT_PREFIX" to "STRING_$stringSize",
        "STRING_SIZE_PLACEHOLDER" to stringSize.toString(),
    )

    override fun getTargetFilename() = "string_$stringSize.zn"
}

data class ByteArrayTemplateParameters(
    val arraySize: Int
) : TemplateParameters("byte_array.zn", emptyList()) {
    override fun getReplacements() = mapOf(
        "ARRAY_LENGTH" to "$arraySize",
    )

    override fun getTargetFilename() = "byte_array_$arraySize.zn"
}

data class PublicKeyTemplateParameters(
    override val templateFile: String,
    val typeName: String,
    val encodedSize: Int,
) : TemplateParameters(
    templateFile,
    listOf(
        StringTemplateParameters(1),
        ByteArrayTemplateParameters(encodedSize),
    )
) {
    private val snakeCaseType by lazy {
        typeName.camelToSnakeCase().replace("public_key", "_public_key")
    }

    override fun getReplacements() = mapOf(
        "TYPE_NAME" to typeName,
        "CONST_PREFIX" to snakeCaseType.toUpperCase(),
        "ENCODED_SIZE" to "$encodedSize",
        "TYPE_SIZE" to "1",
    )

    override fun getTargetFilename() = "$snakeCaseType.zn"

    companion object {
        private const val TEMPLATE = "public_key.zn"
        private const val TEMPLATE_WITH_SCHEME_ID = "public_key_with_scheme_id.zn"

        val all = listOf(
            PublicKeyTemplateParameters(TEMPLATE, "BCRSAPublicKey", BCRSASurrogate.ENCODED_SIZE),
            PublicKeyTemplateParameters(TEMPLATE_WITH_SCHEME_ID, "BCECPublicKey", BCECSurrogate.ENCODED_SIZE),
            PublicKeyTemplateParameters(TEMPLATE, "EdDSAPublicKey", EdDSASurrogate.ENCODED_SIZE),
            PublicKeyTemplateParameters(TEMPLATE, "BCSphincs256PublicKey", BCSphincs256Surrogate.ENCODED_SIZE)
        )
    }
}

data class AbstractPartyTemplateParameters(val pkTemplateParameters: PublicKeyTemplateParameters) : TemplateParameters(
    "abstract_party.zn",
    listOf(pkTemplateParameters)
) {
    private val pkTypeName by lazy { pkTemplateParameters.typeName }
    private val algName by lazy { pkTypeName.removeSuffix("PublicKey") }
    private val pkSnakeCaseType by lazy { pkTypeName.camelToSnakeCase().replace("public_key", "_public_key") }
    private val typeName by lazy { "AbstractParty$algName" }

    override fun getReplacements() = mapOf(
        "TYPE_NAME" to typeName,
        "CONSTANT_PREFIX" to typeName.camelToSnakeCase().toUpperCase(),
        "PK_TYPE_NAME" to pkTypeName,
        "PK_CONSTANT_PREFIX" to pkSnakeCaseType.toUpperCase(),
        "PK_MODULE_NAME" to pkSnakeCaseType,
    )

    override fun getTargetFilename() = "${typeName.camelToSnakeCase()}.zn"

    companion object {
        val all = PublicKeyTemplateParameters.all.map {
            AbstractPartyTemplateParameters(it)
        }
    }
}

data class AnonymousPartyTemplateParameters(val pkTemplateParameters: PublicKeyTemplateParameters) : TemplateParameters(
    "anonymous_party.zn",
    listOf(pkTemplateParameters)
) {
    private val pkTypeName by lazy { pkTemplateParameters.typeName }
    private val algName by lazy { pkTypeName.removeSuffix("PublicKey") }
    private val pkSnakeCaseType by lazy { pkTypeName.camelToSnakeCase().replace("public_key", "_public_key") }
    private val typeName by lazy { "AnonymousParty$algName" }

    override fun getReplacements() = mapOf(
        "TYPE_NAME" to typeName,
        "CONSTANT_PREFIX" to typeName.camelToSnakeCase().toUpperCase(),
        "PK_TYPE_NAME" to pkTypeName,
        "PK_CONSTANT_PREFIX" to pkSnakeCaseType.toUpperCase(),
        "PK_MODULE_NAME" to pkSnakeCaseType,
    )

    override fun getTargetFilename() = "${typeName.camelToSnakeCase()}.zn"

    companion object {
        val all = PublicKeyTemplateParameters.all.map {
            AnonymousPartyTemplateParameters(it)
        }
    }
}

data class PartyTemplateParameters(val pkTemplateParameters: PublicKeyTemplateParameters) : TemplateParameters(
    "party.zn",
    listOf(pkTemplateParameters)
) {
    private val pkTypeName by lazy { pkTemplateParameters.typeName }
    private val algName by lazy { pkTypeName.removeSuffix("PublicKey") }
    private val pkSnakeCaseType by lazy { pkTypeName.camelToSnakeCase().replace("public_key", "_public_key") }
    private val typeName by lazy { "Party$algName" }

    override fun getReplacements() = mapOf(
        "TYPE_NAME" to typeName,
        "CONSTANT_PREFIX" to typeName.camelToSnakeCase().toUpperCase(),
        "PK_TYPE_NAME" to pkTypeName,
        "PK_CONSTANT_PREFIX" to pkSnakeCaseType.toUpperCase(),
        "PK_MODULE_NAME" to pkSnakeCaseType,
    )

    override fun getTargetFilename() = "${typeName.camelToSnakeCase()}.zn"

    companion object {
        val all = PublicKeyTemplateParameters.all.map {
            PartyTemplateParameters(it)
        }
    }
}

object UniqueIdentifierTemplateParameters : TemplateParameters(
    "unique_identifier.zn",
    listOf(StringTemplateParameters(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH.toShort()))
) {
    override fun getReplacements() = mapOf(
        "EXTERNAL_ID_STRING_SIZE" to UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH.toString(),
    )
}

object LinearPointerTemplateParameters : TemplateParameters(
    "linear_pointer.zn",
    listOf(StringTemplateParameters(LinearPointerSurrogate.MAX_CLASS_NAME_SIZE.toShort()))
) {
    override fun getReplacements() = mapOf(
        "CLASS_NAME_STRING_SIZE" to LinearPointerSurrogate.MAX_CLASS_NAME_SIZE.toString(),
    )
}

object X500PrincipalTemplateParameters : TemplateParameters(
    "x500_principal.zn",
    listOf(StringTemplateParameters(X500PrincipalSurrogate.PRINCIPAL_SIZE.toShort()))
) {
    override fun getReplacements() = mapOf(
        "PRINCIPAL_NAME_STRING_SIZE" to X500PrincipalSurrogate.PRINCIPAL_SIZE.toString(),
    )
}

object CurrencyTemplateParameters : TemplateParameters(
    "currency.zn",
    listOf(StringTemplateParameters(CurrencySurrogate.CURRENCY_SIZE.toShort()))
) {
    override fun getReplacements() = mapOf(
        "CURRENCY_CODE_STRING_SIZE" to CurrencySurrogate.CURRENCY_SIZE.toString(),
    )
}

object SecureHashTemplateParameters : TemplateParameters(
    "secure_hash.zn",
    listOf(ByteArrayTemplateParameters(SecureHashSurrogate.BYTES_SIZE))
) {
    override fun getReplacements() = mapOf(
        "BYTES_SIZE" to SecureHashSurrogate.BYTES_SIZE.toString(),
    )
}

private val camelRegex = "(?<=[a-z])[A-Z]".toRegex()

internal fun String.camelToSnakeCase(): String {
    return camelRegex.replace(this) {
        "_${it.value}"
    }.toLowerCase()
}
