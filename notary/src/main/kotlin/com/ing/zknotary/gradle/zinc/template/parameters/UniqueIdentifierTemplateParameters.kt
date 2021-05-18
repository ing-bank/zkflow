package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

object UniqueIdentifierTemplateParameters : TemplateParameters(
    "unique_identifier.zn",
    listOf(StringTemplateParameters(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH.toShort()))
) {
    override fun getReplacements() = mapOf(
        "EXTERNAL_ID_STRING_SIZE" to UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH.toString(),
    )
}
