package com.ing.zkflow.gradle.zinc.template.parameters

import com.ing.zkflow.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zkflow.gradle.zinc.template.TemplateParameters

object LinearPointerTemplateParameters : TemplateParameters(
    "linear_pointer.zn",
    listOf(ByteArrayTemplateParameters(CordaSerializers.CLASS_NAME_SIZE))
) {
    override fun getReplacements() = mapOf(
        "CLASS_NAME_BYTE_ARRAY_SIZE" to CordaSerializers.CLASS_NAME_SIZE.toString(),
    )
}
