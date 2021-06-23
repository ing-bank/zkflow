package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class CollectionTemplateParameters<T>(
    val baseTemplateFile: String = "collection.zn",
    val collectionSize: Int,
    val innerTemplateParameters: T? = null,
    val platformModuleName: String? = null,
) : TemplateParameters(
    baseTemplateFile,
    listOfNotNull(innerTemplateParameters)
) where T : TemplateParameters {
    private val innerModuleName = innerTemplateParameters?.getModuleName()
        ?: requireNotNull(platformModuleName) { "Module name must be provided for non-templated zinc sources" }

    private val innerTypeName = innerTemplateParameters?.typeName
        ?: innerModuleName.removePrefix("platform_").snakeCaseToCamel()

    private val numberPad = "_".takeIf { innerTypeName.matches(".*[\\d|[A-Z]]".toRegex()) } ?: ""

    override val typeName = "${innerTypeName}${numberPad}Collection_$collectionSize"

    override fun getTargetFilename() = getFileName()

    override fun getReplacements() = getTypeReplacements() + (
        innerTemplateParameters?.getTypeReplacements("INNER_") ?: mapOf(
            "INNER_TYPE_NAME" to innerTypeName,
            "INNER_CONSTANT_PREFIX" to innerModuleName.removePrefix("platform_").toUpperCase(),
            "INNER_MODULE_NAME" to innerModuleName,
        )
        ).plus("FIXED_SIZE" to "$collectionSize")
}
