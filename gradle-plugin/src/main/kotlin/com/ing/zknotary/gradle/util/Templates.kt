package com.ing.zknotary.gradle.util

import java.io.File

class Templates(private val circuitName: String, private val mergedCircuitOutput: File, private val circuitSourcesBase: File) {

    var templateContents: String = " "

    fun generateFloatingPointsCode(bigDecimalSizes: Set<Pair<Int, Int>>) {
        bigDecimalSizes.forEach {
            val floatingPointContent = templateContents.replace("\${INTEGER_SIZE_PLACEHOLDER}", it.first.toString())
                .replace("\${FRACTION_SIZE_PLACEHOLDER}", it.second.toString())
            val sizeSuffix = "${it.first}_${it.second}"
            val targetFile = if (circuitName == "test") {
                mergedCircuitOutput.resolve("floating_point_$sizeSuffix.zn") // for test code
            } else {
                mergedCircuitOutput.resolve(circuitName).resolve("src").resolve("floating_point_$sizeSuffix.zn")
            }

            targetFile.parentFile?.mkdirs()
            targetFile.delete()
            targetFile.createNewFile()
            targetFile.writeBytes(floatingPointContent.toByteArray())
        }
    }

    fun generateMerkleUtilsCode() {
        val targetFile = mergedCircuitOutput.resolve(circuitName).resolve("src").resolve("merkle_utils.zn")

        targetFile.parentFile?.mkdirs()
        targetFile.delete()
        targetFile.createNewFile()
        targetFile.writeText("//! Limited-depth recursion for Merkle tree construction\n")
        targetFile.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zk-notary GenerateZincPlatformCodeFromTemplatesTask.kt\n")
        targetFile.appendText(
            """
mod platform_component_group_leaf_digest_dto;
mod platform_crypto_utils;
mod platform_node_digest_dto;

use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestBits;
use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestDto;
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

        val constsContent = circuitSourcesBase.resolve(circuitName).resolve("consts.zn").readText()
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

    fun generateMainCode() {
        val targetFile = mergedCircuitOutput.resolve(circuitName).resolve("src").resolve("main.zn")
        targetFile.parentFile?.mkdirs()
        targetFile.delete()
        targetFile.createNewFile()
        targetFile.writeText("//! GENERATED CODE. DO NOT EDIT\n//! Edit it in zk-notary GenerateZincPlatformCodeFromTemplatesTask.kt\n")
        targetFile.appendText("//! The '$circuitName' main module.")
        targetFile.appendText(
            """
mod consts;
mod contract_rules;
mod platform_component_group_leaf_digest_dto;
mod platform_merkle_tree;
mod platform_node_digest_dto;
mod platform_utxo_digests;
mod platform_zk_prover_transaction;
 
use consts::INPUT_GROUP_SIZE;
use consts::REFERENCE_GROUP_SIZE; 
use contract_rules::check_contract_rules;
use platform_component_group_leaf_digest_dto::ComponentGroupLeafDigestDto;
use platform_component_group_leaf_digest_dto::COMPONENT_GROUP_LEAF_DIGEST_BYTES;
use platform_merkle_tree::build_merkle_tree;
use platform_node_digest_dto::NodeDigestDto;
use platform_node_digest_dto::NODE_DIGEST_BYTES;
use platform_utxo_digests::compute_input_utxo_digests;
use platform_utxo_digests::compute_reference_utxo_digests;
use platform_zk_prover_transaction::Witness;
"""
        )
        val constsContent = circuitSourcesBase.resolve(circuitName).resolve("consts.zn").readText()

        val inputHashes = getUtxoDigestCode(constsContent, "input")
        val referenceHashes = getUtxoDigestCode(constsContent, "reference")

        val mainContent = templateContents.replace("\${COMMAND_NAME_PLACEHOLDER}", circuitName)
            .replace("\${INPUT_HASH_PLACEHOLDER}", inputHashes)
            .replace("\${REFERENCE_HASH_PLACEHOLDER}", referenceHashes)
        targetFile.appendBytes(mainContent.toByteArray())
    }

    private fun getUtxoDigestCode(constsContent: String, componentGroupName: String): String {
        val componentGroupSize = getMerkleTreeSizeForComponent(componentGroupName, constsContent)

        if (componentGroupSize != null) {
            return when {
                componentGroupSize > 0 -> {
                    """compute_${componentGroupName}_utxo_digests( 
            witness.transaction.${componentGroupName}s.components,
            witness.${componentGroupName}_nonces,
        )"""
                }
                componentGroupSize == 0 -> {
                    """[ComponentGroupLeafDigestDto {
            bytes: [0; COMPONENT_GROUP_LEAF_DIGEST_BYTES],
        }; ${componentGroupName.toUpperCase()}_GROUP_SIZE]"""
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
