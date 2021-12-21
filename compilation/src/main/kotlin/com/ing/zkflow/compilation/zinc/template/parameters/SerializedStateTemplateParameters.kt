package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.common.zkp.metadata.ZincType
import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.util.camelToSnakeCase

data class SerializedStateTemplateParameters(
    val componentName: String,
    val typeCount: Int,
    val zincType: ZincType
) : TemplateParameters(
    "serialized_tx_state.zn",
    listOf()
) {
    override val typeName = zincType.typeName

    // TODO: Consider adding the command/circuit name to this filename to better support multiple commands. Waiting for ZincPoet
    override fun getTargetFilename() = "serialized_${componentName}_tx_state_${typeName.camelToSnakeCase()}.zn"

    override fun getReplacements() = getTypeReplacements("STATE_NAME_") +
        mapOf(
            "COMPONENT_NAME_CONSTANT_PREFIX" to componentName.toUpperCase(),
            "COMPONENT_NAME_TYPE_NAME" to componentName.capitalize(),
            "GROUP_SIZE_PLACEHOLDER" to typeCount.toString(),
        ) +
        if (componentName.contains("output")) {
            mapOf(
                "COMPONENT_TYPE_CONSTANT_PREFIX" to "COMPONENT",
                "COMPONENT_TYPE_MODULE_NAME" to "component_leaf",
                "COMPONENT_TYPE_TYPE_NAME" to "Group",

                "NONCE_USE_PLACEHOLDER" to
                    """use platform_crypto_utils::compute_nonce;
use platform_privacy_salt::PrivacySaltBits;
                    """.trimIndent(),

                "NONCE_CALL_PLACEHOLDER" to
                    """privacy_salt: PrivacySaltBits,
    component_group_index: u32""",

                "NONCE_FUNCTION_PLACEHOLDER" to
                    "compute_nonce(privacy_salt, component_group_index, i + element_index as u32),"
            )
        } else {
            mapOf(
                "COMPONENT_TYPE_CONSTANT_PREFIX" to "UTXO",
                "COMPONENT_TYPE_MODULE_NAME" to "utxo",
                "COMPONENT_TYPE_TYPE_NAME" to "Utxos",

                "NONCE_USE_PLACEHOLDER" to
                    """use platform_nonce_digest_dto::from_bytes_to_bits;
use platform_nonce_digest_dto::NonceDigestBytes;
                    """.trimIndent(),

                "NONCE_CALL_PLACEHOLDER" to
                    "nonces: [NonceDigestBytes; ${componentName.toUpperCase()}_GROUP_SIZE]",

                "NONCE_FUNCTION_PLACEHOLDER" to
                    "from_bytes_to_bits(nonces[element_index + i]),"
            )
        }
}
