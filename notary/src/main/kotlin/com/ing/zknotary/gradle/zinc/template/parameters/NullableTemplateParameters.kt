package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.NamedType
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

class NullableTemplateParameters<T>(
    private val innerTemplateParameters: T? = null,
    platformModuleName: String? = null,
) : NamedType, TemplateParameters(
    "nullable.zn",
    listOfNotNull(innerTemplateParameters)
) where T : TemplateParameters {
    // The parameters for the inner structure can be provided in 3 ways:
    // 1) as a class implementing NamedType and inheriting from TemplateParameters
    // 2) as a class inheriting only from TemplateParameters
    // 3) as a simple module name (String) in the case of platform sources
    private val innerModuleName = innerTemplateParameters?.let {
        when (it) {
            is NamedType -> it.getModuleName()
            else -> it.templateFile.removeSuffix(".zn")
        }
    } ?: requireNotNull(platformModuleName) { "Module name must be provided for non-templated zinc sources" }

    private val innerTypeName = innerTemplateParameters?.let {
        when (it) {
            is NamedType -> it.typeName
            else -> innerModuleName.snakeCaseToCamel()
        }
    } ?: innerModuleName.removePrefix("platform_").snakeCaseToCamel()

    override val typeName: String = "Nullable$innerTypeName"

    private val replacementMap = mapOf(
        "INNER_TYPE_NAME" to innerTypeName,
        "INNER_CONSTANT_PREFIX" to innerModuleName.removePrefix("platform_").toUpperCase(),
        "INNER_MODULE_NAME" to innerModuleName,
    )

    override fun getTargetFilename() = getFileName()
    override fun getReplacements() = innerTemplateParameters?.let {
        when (it) {
            is NamedType -> it.getTypeReplacements("INNER_")
            else -> replacementMap
        }
    } ?: replacementMap

    companion object {
        // fixed nullable template parameters needed for platform sources
        val fixed = listOf(
            // needed for TimeWindow
            NullableTemplateParameters(null, "platform_instant"),
        )
    }
}
