package com.ing.zkflow.gradle.zinc.template.parameters

import com.ing.zkflow.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zkflow.gradle.zinc.template.TemplateParameters

object SecureHashTemplateParameters : TemplateParameters(
    "secure_hash.zn",
    listOf(ByteArrayTemplateParameters(SecureHashSurrogate.BYTES_SIZE))
) {
    override fun getReplacements() = mapOf(
        "BYTES_SIZE" to SecureHashSurrogate.BYTES_SIZE.toString(),
    )
}
