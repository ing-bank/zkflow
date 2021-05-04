package com.ing.zknotary.gradle.template

import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.gradle.util.createOutputFile
import java.nio.file.Path

class TemplateRenderer(
    private val outputDirectory: Path,
    private val templateLoader: TemplateLoader
) {
    fun renderTemplate(templateParameters: TemplateParameters) {
        val templateContents =
            renderTemplateWarning(templateParameters) + templateLoader.loadTemplate(templateParameters)
        when (templateParameters) {
            is StringTemplateParameters -> renderStringTemplate(templateContents, templateParameters)
            is BigDecimalTemplateParameters -> renderBigDecimalTemplate(templateContents, templateParameters)
            is AmountTemplateParameters -> renderAmountTemplate(templateContents, templateParameters)
            is UniqueIdentifierTemplateParameters -> renderUniqueIdentifierTemplate(templateContents, templateParameters)
            is LinearPointerTemplateParameters -> renderLinearPointerTemplate(templateContents, templateParameters)
            is X500PrincipalTemplateParameters -> renderX500PrincipalTemplate(templateContents, templateParameters)
            is CurrencyTemplateParameters -> renderCurrencyTemplate(templateContents, templateParameters)
        }
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
            .replace("\${PRINCIPAL_NAME_STRING_SIZE}", "1024")
        createOutputFile(outputDirectory.resolve(templateParameters.templateFile))
            .writeBytes(linearPointerContent.toByteArray())
    }

    private fun renderCurrencyTemplate(
        templateContents: String,
        templateParameters: CurrencyTemplateParameters
    ) {
        val linearPointerContent = templateContents
            .replace("\${CURRENCY_CODE_STRING_SIZE}", "3")
        createOutputFile(outputDirectory.resolve(templateParameters.templateFile))
            .writeBytes(linearPointerContent.toByteArray())
    }
}