package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.gradle.zinc.template.NamedType
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class PublicKeyTemplateParameters(
    override val templateFile: String,
    override val typeName: String,
    val encodedSize: Int,
) : NamedType, TemplateParameters(
    templateFile,
    listOf(
        StringTemplateParameters(1),
        ByteArrayTemplateParameters(encodedSize),
    )
) {
    override fun getModuleName() = super.getModuleName().replace("public_key", "_public_key")

    override fun getReplacements() = getTypeReplacements() + mapOf(
        "ENCODED_SIZE" to "$encodedSize",
        "TYPE_SIZE" to "1",
    )

    override fun getTargetFilename() = getFileName()

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
