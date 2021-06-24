package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

object LinearPointerTemplateParameters : TemplateParameters(
    "linear_pointer.zn",
    listOf(StringTemplateParameters(CordaSerializers.CLASS_NAME_SIZE))
) {
    override fun getReplacements() = mapOf(
        "CLASS_NAME_STRING_SIZE" to CordaSerializers.CLASS_NAME_SIZE.toString(),
    )
}
