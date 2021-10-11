package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.serialization.bfl.serializers.UniqueIdentifierSurrogate

object UniqueIdentifierTemplateParameters : TemplateParameters(
    "unique_identifier.zn",
    listOf(
        NullableTemplateParameters(innerTemplateParameters = StringTemplateParameters(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH))
    )
) {
    override fun getReplacements() = mapOf(
        "EXTERNAL_ID_STRING_SIZE" to UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH.toString(),
    )
}
