package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class NullableTemplateParameters<T>(
    private val baseTemplateFile: String = "nullable.zn",
    private val innerTemplateParameters: T? = null,
    private val platformModuleName: String? = null,
) : TemplateParameters(
    baseTemplateFile,
    listOfNotNull(innerTemplateParameters)
) where T : TemplateParameters {
    // The parameters for the inner structure can be provided in 2 ways:
    // 1) as a class inheriting from TemplateParameters
    // 2) as a simple module name (String) in the case of platform sources
    private val innerModuleName = innerTemplateParameters?.getModuleName()
        ?: requireNotNull(platformModuleName) { "Module name must be provided for non-templated zinc sources" }

    private val innerTypeName = innerTemplateParameters?.typeName
        ?: innerModuleName.removePrefix("platform_").snakeCaseToCamel()

    // extract nullable or optional prefix from the templateFile name
    val prefix = templateFile.removeSuffix(".zn").split("_")[0].firstCharToUpperCase()
    override val typeName: String = "$prefix$innerTypeName"

    override fun getTargetFilename() = getFileName()
    override fun getReplacements() = innerTemplateParameters?.getTypeReplacements("INNER_")
        ?: mapOf(
            "INNER_TYPE_NAME" to innerTypeName,
            "INNER_CONSTANT_PREFIX" to innerModuleName.removePrefix("platform_").toUpperCase(),
            "INNER_MODULE_NAME" to innerModuleName,
        )

    companion object {
        // fixed nullable template parameters needed for platform sources
        // NOTE: the cases of u8, and i8 are handled separately in a hardcoded manner
        val fixed = listOf("i16", "u16", "i32", "u32", "i64", "u64", "i128", "u128").map {
            NullableTemplateParameters("nullable_integer.zn", null, it)
        }.plus(
            // needed for TimeWindow
            NullableTemplateParameters(innerTemplateParameters = null, platformModuleName = "platform_instant")
        )
    }
}
