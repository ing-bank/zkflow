package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.getFullMerkleTreeSize
import com.ing.zknotary.gradle.util.getMerkleTreeSizeForComponent
import com.ing.zknotary.gradle.util.platformSources
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        generateFloatingPointCode()
        generateMerkleUtilsCode()
        generateMainCode()
    }

    private fun generateFloatingPointCode() {
        val templateContents = project.getTemplateContents("floating_point.zn")

        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            extension.bigDecimalSizes.forEach {
                val floatingPointContent = templateContents.replace("\${INTEGER_SIZE_PLACEHOLDER}", it.first.toString())
                    .replace("\${FRACTION_SIZE_PLACEHOLDER}", it.second.toString())
                val sizeSuffix = "${it.first}_${it.second}"
                val targetFile = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src/floating_point_$sizeSuffix.zn")
                targetFile.delete()
                targetFile.createNewFile()
                targetFile.writeBytes(floatingPointContent.toByteArray())
            }
        }
    }

    private fun generateMerkleUtilsCode() {
        val templateContents = project.getTemplateContents("merkle_template.zn")
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val targetFile = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src/merkle_utils.zn")
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
            val constsTemplate = File("${extension.circuitSourcesBasePath.resolve(circuitName)}/consts.zn").readText()
            val fullMerkleLeaves = getFullMerkleTreeSize(constsTemplate)
            targetFile.appendText(
                getMerkleTree(
                    templateContents, fullMerkleLeaves, digestType = "node_digests",
                    digestBitsType = "NodeDigestBits",
                    digestBits = "NODE_DIGEST_BITS",
                    dto = "NodeDigestDto"
                )
            )

            targetFile.appendText(
                getMerkleTree(
                    templateContents, fullMerkleLeaves, digestType = "component_group_leaf_digests",
                    digestBitsType = "ComponentGroupLeafDigestBits",
                    digestBits = "COMPONENT_GROUP_LEAF_DIGEST_BITS",
                    dto = "ComponentGroupLeafDigestDto"
                )
            )
        }
    }

    private fun getMerkleTree(templateContents: String, fullMerkleLeaves: Int, digestType: String, digestBitsType: String, digestBits: String, dto: String): String {
        var digestMerkleFunctions = ""
        // Compute the root
        digestMerkleFunctions +=
            """
fn get_merkle_tree_from_2_$digestType(leaves: [$digestBitsType; 2]) -> $digestBitsType {
    pedersen_to_padded_bits(pedersen(concatenate_$digestType(leaves[0], leaves[1])).0)
}
"""
        if (fullMerkleLeaves > 2) {
            var leaves = 4
            do {
                val levelUp = leaves / 2
                digestMerkleFunctions += templateContents.replace("\${NUM_LEAVES_PLACEHOLDER}", leaves.toString())
                    .replace("\${DIGEST_TYPE_PLACEHOLDER}", digestType)
                    .replace("\${DIGEST_BITS_TYPE_PLACEHOLDER}", digestBitsType)
                    .replace("\${DIGEST_BITS_PLACEHOLDER}", digestBits)
                    .replace("\${DTO_PLACEHOLDER}", dto)
                    .replace("\${LEVEL_UP_PLACEHOLDER}", levelUp.toString())
                leaves *= 2
            } while (leaves <= fullMerkleLeaves)
        }
        return digestMerkleFunctions
    }

    private fun generateMainCode() {
        val templateContents = project.getTemplateContents("main_template.zn")

        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val targetFile = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src/main.zn")
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

            val constsTemplate = File("${extension.circuitSourcesBasePath.resolve(circuitName)}/consts.zn").readText()
            val inputHashes = replaceComponentPlaceholders(constsTemplate, "input")
            val referenceHashes = replaceComponentPlaceholders(constsTemplate, "reference")

            val mainContent = templateContents.replace("\${COMMAND_NAME_PLACEHOLDER}", circuitName)
                .replace("\${INPUT_HASH_PLACEHOLDER}", inputHashes)
                .replace("\${REFERENCE_HASH_PLACEHOLDER}", referenceHashes)
            targetFile.appendBytes(mainContent.toByteArray())
        }
    }

    private fun replaceComponentPlaceholders(template: String, componentGroup: String): String {
        val componentGroupSize = getMerkleTreeSizeForComponent(componentGroup, template)

        if (componentGroupSize != null) {
            return when {
                componentGroupSize > 0 -> {
                    """compute_${componentGroup}_utxo_digests( 
            witness.transaction.${componentGroup}s.components,
            witness.${componentGroup}_nonces,
        )"""
                }
                componentGroupSize == 0 -> {
                    """[ComponentGroupLeafDigestDto {
            bytes: [0; COMPONENT_GROUP_LEAF_DIGEST_BYTES],
        }; ${componentGroup.toUpperCase()}_GROUP_SIZE]"""
                }
                else -> {
                    throw IllegalArgumentException("Negative values are not allowed for ${componentGroup.toUpperCase()}_GROUP_SIZE in consts.zn")
                }
            }
        } else {
            throw IllegalArgumentException("Unknown value for ${componentGroup.toUpperCase()}_GROUP_SIZE in consts.zn")
        }
    }

    private fun Project.getTemplateContents(templateFileName: String): String {
        return project.platformSources.matching {
            it.include("zinc-platform-templates/$templateFileName")
        }.singleFile.readText()
    }
}
