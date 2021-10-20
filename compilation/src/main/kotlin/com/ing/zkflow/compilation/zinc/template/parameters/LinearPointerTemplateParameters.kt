package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers

object LinearPointerTemplateParameters : TemplateParameters(
    "linear_pointer.zn",
    listOf(ByteArrayTemplateParameters(CordaSerializers.CLASS_NAME_SIZE))
) {
    override fun getReplacements() = mapOf(
        "CLASS_NAME_BYTE_ARRAY_SIZE" to CordaSerializers.CLASS_NAME_SIZE.toString(),
    )
}
