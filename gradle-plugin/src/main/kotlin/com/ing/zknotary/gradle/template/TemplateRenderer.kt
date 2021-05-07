package com.ing.zknotary.gradle.template

import com.ing.serialization.bfl.serializers.CurrencySurrogate
import com.ing.serialization.bfl.serializers.X500PrincipalSurrogate
import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.gradle.util.createOutputFile
import java.nio.file.Path

@Suppress("TooManyFunctions")
class TemplateRenderer(
    private val outputDirectory: Path,
    private val templateLoader: TemplateLoader
) {
    fun renderTemplate(templateParameters: TemplateParameters) {
        val templateContents =
            renderTemplateWarning(templateParameters) + templateLoader.loadTemplate(templateParameters)
        when (templateParameters) {
            is StringTemplateParameters -> renderStringTemplate(templateContents, templateParameters)
            is ByteArrayTemplateParameters -> renderByteArrayTemplate(templateContents, templateParameters)
            is BigDecimalTemplateParameters -> renderBigDecimalTemplate(templateContents, templateParameters)
            is AmountTemplateParameters -> renderAmountTemplate(templateContents, templateParameters)
            is UniqueIdentifierTemplateParameters -> renderUniqueIdentifierTemplate(templateContents, templateParameters)
            is LinearPointerTemplateParameters -> renderLinearPointerTemplate(templateContents, templateParameters)
            is X500PrincipalTemplateParameters -> renderX500PrincipalTemplate(templateContents, templateParameters)
            is CurrencyTemplateParameters -> renderCurrencyTemplate(templateContents, templateParameters)
            is SecureHashTemplateParameters -> renderSecureHashTemplate(templateContents, templateParameters)
            is PublicKeyTemplateParameters -> renderPublicKeyTemplate(templateContents, templateParameters)
        }
    }

    private fun renderPublicKeyTemplate(templateContents: String, templateParameters: PublicKeyTemplateParameters) {
        val snakeCaseType = templateParameters.typeName.camelToSnakeCase().replace("public_key", "_public_key")
        val stringContent = templateContents
            .replace("\${TYPE_NAME}", templateParameters.typeName)
            .replace("\${CONST_PREFIX}", snakeCaseType.toUpperCase())
            .replace("\${ENCODED_SIZE}", "${templateParameters.encodedSize}")
            .replace("\${SERIAL_NAME_SIZE}", "1")
        createOutputFile(outputDirectory.resolve("$snakeCaseType.zn"))
            .writeBytes(stringContent.toByteArray())
    }

    private fun renderTemplateWarning(
        templateParameters: TemplateParameters
    ): String {
        return """
            //! GENERATED CODE. DO NOT EDIT
            //! Edit the template in zinc-platform-sources/src/main/resources/zinc-platform-templates/${templateParameters.templateFile}
            //
        """.trimIndent() + "\n"
    }

    private fun renderStringTemplate(
        templateContents: String,
        templateParameters: StringTemplateParameters
    ) {
        val sizeSuffix = "${templateParameters.stringSize}"
        val stringContent = templateContents
            .replace("\${TYPE_NAME}", "String_$sizeSuffix")
            .replace("\${CONSTANT_PREFIX}", "STRING_$sizeSuffix")
            .replace("\${STRING_SIZE_PLACEHOLDER}", templateParameters.stringSize.toString())
        createOutputFile(outputDirectory.resolve("string_$sizeSuffix.zn"))
            .writeBytes(stringContent.toByteArray())
    }

    private fun renderByteArrayTemplate(
        templateContents: String,
        templateParameters: ByteArrayTemplateParameters
    ) {
        val sizeSuffix = "${templateParameters.arraySize}"
        val stringContent = templateContents
            .replace("\${ARRAY_LENGTH}", sizeSuffix)
        createOutputFile(outputDirectory.resolve("byte_array_$sizeSuffix.zn"))
            .writeBytes(stringContent.toByteArray())
    }

    private fun renderBigDecimalTemplate(
        templateContents: String,
        templateParameters: BigDecimalTemplateParameters
    ) {
        val bigDecimalsContent = templateContents
            .replace("\${TYPE_NAME}", templateParameters.typeName)
            .replace("\${CONSTANT_PREFIX}", templateParameters.typeName.camelToSnakeCase().toUpperCase())
            .replace("\${INTEGER_SIZE_PLACEHOLDER}", templateParameters.integerSize.toString())
            .replace("\${FRACTION_SIZE_PLACEHOLDER}", templateParameters.fractionSize.toString())
        createOutputFile(outputDirectory.resolve("${templateParameters.typeName.camelToSnakeCase()}.zn"))
            .writeBytes(bigDecimalsContent.toByteArray())
    }

    private fun renderAmountTemplate(
        templateContents: String,
        templateParameters: AmountTemplateParameters
    ) {
        val bigDecimalsContent = templateContents
            .replace("\${TYPE_NAME}", templateParameters.typeName)
            .replace("\${CONSTANT_PREFIX}", templateParameters.typeName.camelToSnakeCase().toUpperCase())
            .replace("\${TOKEN_SIZE_PLACEHOLDER}", templateParameters.tokenSize.toString())
            .replace("\${BD_TYPE_NAME}", templateParameters.tokenDisplaySize.typeName)
            .replace(
                "\${BD_CONSTANT_PREFIX}",
                templateParameters.tokenDisplaySize.typeName.camelToSnakeCase().toUpperCase()
            )
            .replace("\${BD_MODULE_NAME}", templateParameters.tokenDisplaySize.typeName.camelToSnakeCase())
        createOutputFile(outputDirectory.resolve("${templateParameters.typeName.camelToSnakeCase()}.zn"))
            .writeBytes(bigDecimalsContent.toByteArray())
    }

    private fun renderUniqueIdentifierTemplate(
        templateContents: String,
        templateParameters: UniqueIdentifierTemplateParameters
    ) {
        val uniqueIdentifierContent = templateContents
            .replace("\${EXTERNAL_ID_STRING_SIZE}", UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH.toString())
        createOutputFile(outputDirectory.resolve(templateParameters.templateFile))
            .writeBytes(uniqueIdentifierContent.toByteArray())
    }

    private fun renderLinearPointerTemplate(
        templateContents: String,
        templateParameters: LinearPointerTemplateParameters
    ) {
        val linearPointerContent = templateContents
            .replace("\${CLASS_NAME_STRING_SIZE}", LinearPointerSurrogate.MAX_CLASS_NAME_SIZE.toString())
        createOutputFile(outputDirectory.resolve(templateParameters.templateFile))
            .writeBytes(linearPointerContent.toByteArray())
    }

    private fun renderX500PrincipalTemplate(
        templateContents: String,
        templateParameters: X500PrincipalTemplateParameters
    ) {
        val linearPointerContent = templateContents
            .replace("\${PRINCIPAL_NAME_STRING_SIZE}", X500PrincipalSurrogate.PRINCIPAL_SIZE.toString())
        createOutputFile(outputDirectory.resolve(templateParameters.templateFile))
            .writeBytes(linearPointerContent.toByteArray())
    }

    private fun renderCurrencyTemplate(
        templateContents: String,
        templateParameters: CurrencyTemplateParameters
    ) {
        val linearPointerContent = templateContents
            .replace("\${CURRENCY_CODE_STRING_SIZE}", CurrencySurrogate.CURRENCY_SIZE.toString())
        createOutputFile(outputDirectory.resolve(templateParameters.templateFile))
            .writeBytes(linearPointerContent.toByteArray())
    }

    private fun renderSecureHashTemplate(
        templateContents: String,
        templateParameters: SecureHashTemplateParameters
    ) {
        val linearPointerContent = templateContents
            .replace("\${BYTES_SIZE}", SecureHashSurrogate.BYTES_SIZE.toString())
        createOutputFile(outputDirectory.resolve(templateParameters.templateFile))
            .writeBytes(linearPointerContent.toByteArray())
    }
}
