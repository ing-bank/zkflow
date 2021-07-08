package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCRSASurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCSphincs256Surrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.gradle.zinc.template.TemplateParameters
import net.corda.core.crypto.Crypto

data class PublicKeyTemplateParameters(
    val baseTemplateFile: String,
    val algName: String,
    val encodedSize: Int,
) : TemplateParameters(
    baseTemplateFile,
    listOf(
        ByteArrayTemplateParameters(encodedSize),
    )
) {
    override val typeName = "${algName}PublicKey"
    override fun getModuleName() = super
        .getModuleName()
        .replace("public_key", "_public_key")
        .replace("__", "_")

    override fun getReplacements() = getTypeReplacements() + mapOf(
        "ENCODED_SIZE" to "$encodedSize",
    )

    override fun getTargetFilename() = getFileName()

    companion object {
        private const val TEMPLATE = "public_key.zn"
        private const val TEMPLATE_WITH_SCHEME_ID = "public_key_with_scheme_id.zn"

        val bcrsaTemplateParameters = PublicKeyTemplateParameters(TEMPLATE, "BCRSA", BCRSASurrogate.ENCODED_SIZE)
        val bcecTemplateParameters =
            PublicKeyTemplateParameters(TEMPLATE_WITH_SCHEME_ID, "BCEC", BCECSurrogate.ENCODED_SIZE)
        val eddsaTemplateParameters = PublicKeyTemplateParameters(TEMPLATE, "EdDSA", EdDSASurrogate.ENCODED_SIZE)
        val sphincs256TemplateParameters =
            PublicKeyTemplateParameters(TEMPLATE, "BCSphincs256", BCSphincs256Surrogate.ENCODED_SIZE)

        val all = listOf(
            bcrsaTemplateParameters,
            bcecTemplateParameters,
            eddsaTemplateParameters,
            sphincs256TemplateParameters
        )

        fun selectParameters(schemeCodeName: String): PublicKeyTemplateParameters =
            when (Crypto.findSignatureScheme(schemeCodeName)) {
                Crypto.RSA_SHA256 -> bcrsaTemplateParameters
                Crypto.ECDSA_SECP256K1_SHA256 -> bcecTemplateParameters
                Crypto.ECDSA_SECP256R1_SHA256 -> bcecTemplateParameters
                Crypto.EDDSA_ED25519_SHA512 -> eddsaTemplateParameters
                Crypto.SPHINCS256_SHA256 -> sphincs256TemplateParameters
                else -> error("Unsupported signature scheme")
            }
    }
}
