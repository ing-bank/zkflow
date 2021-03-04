package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
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

            val constsTemplate = File("${extension.circuitSourcesBasePath.resolve(circuitName)}/consts.zn").readText()
            val merkleLeaves = getMerkleLeaveSize(constsTemplate)

            fun isPow2(num: Int) = num and (num - 1) == 0
            val fullLeaves = run {
                var l = merkleLeaves
                while (!isPow2(l)) {
                    l++
                }
                l
            }

            var leaves = fullLeaves
            var levelUp = leaves / 2

            if (fullLeaves == 2) {
                targetFile.appendText(
                    """
fn get_merkle_tree_from_2_component_group_leaf_digests(leaves: [ComponentGroupLeafDigestBits; 2]) -> ComponentGroupLeafDigestBits {
    dbg!("Consuming 2 leaves");
    dbg!("0: {}", ComponentGroupLeafDigestDto::from_bits_to_bytes(leaves[0]));
    dbg!("1: {}", ComponentGroupLeafDigestDto::from_bits_to_bytes(leaves[1]));
    pedersen_to_padded_bits(pedersen(concatenate_component_group_leaf_digests(leaves[0], leaves[1])).0)
}
"""
                )
            } else {
                do {
                    // The lowest level of the tree is the leaf hashes of component subtrees in ComponentGroupLeafDigest type
                    val merkleLeavesContent = if (leaves == fullLeaves) {
                        templateContents.replace("\${NUM_LEAVES_PLACEHOLDER}", leaves.toString())
                            .replace("\${DIGEST_TYPE_PLACEHOLDER}", "component_group_leaf_digests")
                            .replace("\${DIGEST_BITS_PLACEHOLDER}", "ComponentGroupLeafDigestBits")
                            .replace("\${DTO_PLACEHOLDER}", "ComponentGroupLeafDigestDto")
                            .replace("\${LEVEL_UP_PLACEHOLDER}", levelUp.toString())
                    } else {
                        // The upper levels are NodeDigest type
                        templateContents.replace("\${NUM_LEAVES_PLACEHOLDER}", leaves.toString())
                            .replace("\${DIGEST_TYPE_PLACEHOLDER}", "node_digests")
                            .replace("\${DIGEST_BITS_PLACEHOLDER}", "NodeDigestBits")
                            .replace("\${DTO_PLACEHOLDER}", "NodeDigestDto")
                            .replace("\${LEVEL_UP_PLACEHOLDER}", levelUp.toString())
                    }
                    targetFile.appendBytes(merkleLeavesContent.toByteArray())
                    leaves /= 2
                    levelUp = leaves / 2
                } while (2 < leaves)

                // Compute the root
                targetFile.appendText(
                    """
fn get_merkle_tree_from_2_node_digests(leaves: [NodeDigestBits; 2]) -> NodeDigestBits {
    dbg!("Consuming 2 leaves");
    dbg!("0: {}", NodeDigestDto::from_bits_to_bytes(leaves[0]));
    dbg!("1: {}", NodeDigestDto::from_bits_to_bytes(leaves[1]));
    pedersen_to_padded_bits(pedersen(concatenate_node_digests(leaves[0], leaves[1])).0)
}
"""
                )
            }
        }
    }

    private fun generateMainCode() {
        val templateContents = project.getTemplateContents("main_template.zn")

        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val targetFile = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src/main.zn")
            targetFile.delete()
            targetFile.createNewFile()

            val constsTemplate = File("${extension.circuitSourcesBasePath.resolve(circuitName)}/consts.zn").readText()
            val inputHashes = replaceComponentPlaceholders(constsTemplate, "input")
            val referenceHashes = replaceComponentPlaceholders(constsTemplate, "reference")

            val mainContent = templateContents.replace("\${COMMAND_NAME_PLACEHOLDER}", circuitName)
                .replace("\${INPUT_HASH_PLACEHOLDER}", inputHashes)
                .replace("\${REFERENCE_HASH_PLACEHOLDER}", referenceHashes)
            targetFile.writeBytes(mainContent.toByteArray())
        }
    }

    private fun replaceComponentPlaceholders(template: String, componentGroup: String): String {
        val componentRegex = "${componentGroup.toUpperCase()}_GROUP_SIZE: u16 = (\\d+);".toRegex()
        val componentGroupSize = componentRegex.find(template)?.groupValues?.get(1)?.toInt()

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

    private fun getMerkleLeaveSize(constsTemplate: String): Int {
        val search = "GROUP_SIZE: u16 = (\\d+);".toRegex()
        var total = 0
        search.findAll(constsTemplate).forEach {
            total += it.groupValues[1].toInt()
        }
        return total
    }
    private fun Project.getTemplateContents(templateFileName: String): String {
        return project.platformSources.matching {
            it.include("zinc-platform-templates/$templateFileName")
        }.singleFile.readText()
    }
}
