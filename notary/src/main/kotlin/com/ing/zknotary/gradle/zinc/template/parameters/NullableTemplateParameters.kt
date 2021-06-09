package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

class NullableTemplateParameters<T>(
    baseTemplateFile: String = "nullable.zn",
    innerTemplateParameters: T? = null,
    platformModuleName: String? = null,
) : NullableOrOptionalTemplateParameters<T>(
    baseTemplateFile,
    innerTemplateParameters,
    platformModuleName
) where T : TemplateParameters {
    companion object {
        // fixed nullable template parameters needed for platform sources
        val fixed = listOf("i16", "u16", "i32", "u32", "i64", "u64", "i128", "u128").map {
            NullableTemplateParameters("nullable_integer.zn", null, it)
        }.plus(
            // needed for TimeWindow
            NullableTemplateParameters(innerTemplateParameters = null, platformModuleName = "platform_instant")
        )
    }
}
