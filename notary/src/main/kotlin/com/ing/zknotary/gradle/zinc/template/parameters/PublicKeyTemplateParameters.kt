package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class PublicKeyTemplateParameters(
    override val templateFile: String,
    val typeName: String,
    val encodedSize: Int,
) : TemplateParameters(
    templateFile,
    listOf(
        StringTemplateParameters(1),
        ByteArrayTemplateParameters(encodedSize),
    )
) {
    private val snakeCaseType by lazy {
        typeName.camelToSnakeCase().replace("public_key", "_public_key")
    }

    override fun getReplacements() = mapOf(
        "TYPE_NAME" to typeName,
        "CONST_PREFIX" to snakeCaseType.toUpperCase(),
        "ENCODED_SIZE" to "$encodedSize",
        "TYPE_SIZE" to "1",
    )

    override fun getTargetFilename() = "$snakeCaseType.zn"

    companion object {
        private const val TEMPLATE = "public_key.zn"
        private const val TEMPLATE_WITH_SCHEME_ID = "public_key_with_scheme_id.zn"

        val all = listOf(
            PublicKeyTemplateParameters(TEMPLATE, "BCRSAPublicKey", BCRSASurrogate.ENCODED_SIZE),
            PublicKeyTemplateParameters(TEMPLATE_WITH_SCHEME_ID, "BCECPublicKey", BCECSurrogate.ENCODED_SIZE),
            PublicKeyTemplateParameters(TEMPLATE, "EdDSAPublicKey", EdDSASurrogate.ENCODED_SIZE),
            PublicKeyTemplateParameters(TEMPLATE, "BCSphincs256PublicKey", BCSphincs256Surrogate.ENCODED_SIZE)
        )
    }
}
