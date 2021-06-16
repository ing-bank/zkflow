package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class OptionalTemplateParameters<T>(
    private val baseTemplateFile: String = "optional.zn",
    private val innerTemplateParameters: T? = null,
    private val platformModuleName: String? = null,
) : TypeWrapperTemplateParameters<T>(
    baseTemplateFile,
    innerTemplateParameters,
    platformModuleName
) where T : TemplateParameters {
    companion object {
        // fixed optional template parameters needed for platform sources
        val fixed = listOf("i8", "u8", "i16", "u16", "i32", "u32", "i64", "u64", "i128", "u128").map {
            OptionalTemplateParameters("optional_integer.zn", null, it)
        }
    }
}
