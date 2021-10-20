package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.serialization.bfl.serializers.SecureHashSurrogate

object SecureHashTemplateParameters : TemplateParameters(
    "secure_hash.zn",
    listOf(ByteArrayTemplateParameters(SecureHashSurrogate.BYTES_SIZE))
) {
    override fun getReplacements() = mapOf(
        "BYTES_SIZE" to SecureHashSurrogate.BYTES_SIZE.toString(),
    )
}
