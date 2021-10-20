package com.ing.zkflow.compilation.zinc.util

import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import net.corda.core.crypto.Crypto
import java.io.File

class CodeGenerator(
    private val outputPath: File,
    private val metadata: ResolvedZKTransactionMetadata
) {
    companion object {
        /**
         * There is always exactly one notary.
         */
        const val NOTARY_GROUP_SIZE = 1

        val PUBKEY_SIZES = mapOf(
            Crypto.EDDSA_ED25519_SHA512 to 52
        )

        /**
         * This seems to be always 11. Commands are always objects without serialized properties.
         */
        const val COMMAND_COMPONENT_SIZE = 11
    }

    // TODO: signer size should be determined automatically based on serialized size of the pubkey used
    // Until we have that, we maintain an internal map here to look it up.
    fun generateConstsFile() = createOutputFile(outputPath.resolve("consts.zn")).appendBytes(
        """
const ATTACHMENT_GROUP_SIZE: u16 = ${metadata.attachmentCount};
const INPUT_GROUP_SIZE: u16 = ${metadata.inputs.size};
const OUTPUT_GROUP_SIZE: u16 = ${metadata.outputs.size};
const REFERENCE_GROUP_SIZE: u16 = ${metadata.references.size};
const NOTARY_GROUP_SIZE: u16 = $NOTARY_GROUP_SIZE;
const TIMEWINDOW_GROUP_SIZE: u16 = ${if (metadata.timewindow) 1 else 0};
// This is the size of a single signer and should not contain the Corda SerializationMagic size,
// we use platform_consts::CORDA_SERDE_MAGIC_LENGTH for that

const COMMAND_SIGNER_SIZE: u16 = ${PUBKEY_SIZES[metadata.network.participantSignatureScheme]};

// Component and UTXO sizes cannot be 0, so for not present groups use 1
// A single command per circuit is currently supported.
// TODO: Support for multiple commands is to be implemented.
const COMMAND_COMPONENT_SIZE: u16 = $COMMAND_COMPONENT_SIZE;
const COMMAND_SIGNER_LIST_SIZE: u16 = ${metadata.numberOfSigners};
        """.trimIndent().toByteArray()
    )

    fun generateMerkleUtilsCode(templateContents: String, constsContent: String) {
        val targetFile = createOutputFile(outputPath.resolve("merkle_utils.zn"))
        targetFile.writeText("//! Limited-depth recursion for Merkle tree construction\n")
        targetFile.appendText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zk-notary GenerateZincPlatformCodeFromTemplatesTask.kt\n")
        targetFile.appendText(
            """
mod platform_component_group_leaf_digest_dto;
mod platform_crypto_utils;
mod platform_node_digest_dto;

use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestBits;
use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestBytes;
use platform_component_group_leaf_digest_dto::COMPONENT_GROUP_LEAF_DIGEST_BITS;
use platform_crypto_utils::pedersen_to_padded_bits;
use platform_crypto_utils::concatenate_component_group_leaf_digests;
use platform_crypto_utils::concatenate_node_digests;
use platform_node_digest_dto::NodeDigestBits;
use platform_node_digest_dto::NodeDigestDto;
use platform_node_digest_dto::NODE_DIGEST_BITS;
use std::crypto::pedersen;

        """
        )

        val fullMerkleLeaves = getFullMerkleTreeSize(constsContent)
        targetFile.appendText(
            getMerkleTree(
                templateContents, fullMerkleLeaves, digestSnakeCase = "node_digests",
                digestCamelCase = "NodeDigest",
                digestBits = "NODE_DIGEST_BITS"
            )
        )

        targetFile.appendText(
            getMerkleTree(
                templateContents, fullMerkleLeaves, digestSnakeCase = "component_group_leaf_digests",
                digestCamelCase = "ComponentGroupLeafDigest",
                digestBits = "COMPONENT_GROUP_LEAF_DIGEST_BITS"
            )
        )
    }

    fun generateMainCode(templateContents: String, constsContent: String) {
        val targetFile = createOutputFile(outputPath.resolve("main.zn"))
        targetFile.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zk-notary GenerateZincPlatformCodeFromTemplatesTask.kt\n")
        targetFile.appendText("//! The main module.\n")
        val inputHashesCode = getUtxoDigestCode(constsContent, "input")
        val referenceHashesCode = getUtxoDigestCode(constsContent, "reference")

        val mainContent = templateContents
            .replace("\${INPUT_HASH_PLACEHOLDER}", inputHashesCode)
            .replace("\${REFERENCE_HASH_PLACEHOLDER}", referenceHashesCode)
        targetFile.appendBytes(mainContent.toByteArray())
    }

    private fun getUtxoDigestCode(constsContent: String, componentGroupName: String): String {
        val componentGroupSize = getMerkleTreeSizeForComponent(componentGroupName, constsContent)

        if (componentGroupSize != null) {
            return when {
                componentGroupSize > 0 -> {
                    """compute_${componentGroupName}_utxo_digests(
            witness.serialized_${componentGroupName}_utxos,
            witness.${componentGroupName}_nonces,
        )"""
                }
                componentGroupSize == 0 -> {
                    """[[0; COMPONENT_GROUP_LEAF_DIGEST_BYTES]; ${componentGroupName.toUpperCase()}_GROUP_SIZE]"""
                }
                else -> {
                    throw IllegalArgumentException("Negative values are not allowed for ${componentGroupName.toUpperCase()}_GROUP_SIZE in consts.zn")
                }
            }
        } else {
            throw IllegalArgumentException("Unknown value for ${componentGroupName.toUpperCase()}_GROUP_SIZE in consts.zn")
        }
    }
}
