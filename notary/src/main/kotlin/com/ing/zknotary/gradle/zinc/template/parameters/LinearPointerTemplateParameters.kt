package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSurrogate
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

object LinearPointerTemplateParameters : TemplateParameters(
    "linear_pointer.zn",
    listOf(StringTemplateParameters(LinearPointerSurrogate.MAX_CLASS_NAME_SIZE.toShort()))
) {
    override fun getReplacements() = mapOf(
        "CLASS_NAME_STRING_SIZE" to LinearPointerSurrogate.MAX_CLASS_NAME_SIZE.toString(),
    )
}
