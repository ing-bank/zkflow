package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.getFullMerkleTreeSize
import com.ing.zknotary.gradle.util.getMerkleTreeSizeForComponent
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class PrepareCircuitForCompilationTask : DefaultTask() {

    @TaskAction
    fun prepareCircuitForCompilation() {
        removeDebugCode()
        findCorrespondingMerkleTreeFunctionForComponentGroup()
        findCorrespondingMerkleTreeFunctionForMainTree()
    }

    /**
     * Do not include debug statements into the compilation code
     **/
    private fun removeDebugCode() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            File("${extension.mergedCircuitOutputPath.resolve(circuitName)}/").walk().forEach { file ->
                // Skip directories
                if (file.name.contains(".zn")) {
                    val lines = file.readLines()
                    file.delete()
                    file.createNewFile()

                    lines.filter { s: String -> !s.contains("dbg!") }.map {
                        file.appendText("$it\n")
                    }
                }
            }
        }
    }

    /**
     * Find appropriate merkle tree functions for each component group subtree
     * based on component group size
     **/
    private fun findCorrespondingMerkleTreeFunctionForComponentGroup() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val componentGroupFiles = File("${extension.mergedCircuitOutputPath.resolve(circuitName)}/").walk().filter {
                it.name.contains("platform_components")
            }
            componentGroupFiles.forEach { file ->
                // Filter component group files

                val fileContent = file.readText()
                // Find which component group
                val componentRegex = "struct (\\w+)(ComponentGroup)".toRegex()
                var componentGroupName = componentRegex.find(fileContent)?.groupValues?.get(1)

                if (componentGroupName?.endsWith("s")!!) componentGroupName = componentGroupName.dropLast(1)

                val constsTemplate = File("${extension.circuitSourcesBasePath.resolve(circuitName)}/consts.zn").readText()
                val componentGroupSize = getMerkleTreeSizeForComponent(componentGroupName, constsTemplate)

                // Find the size of the component group in consts.zn
                file.delete()
                file.createNewFile()
                val updatedFileContent = callAppropriateMerkleTreeFunction(componentGroupSize, fileContent)

                file.appendBytes(updatedFileContent.toByteArray())
            }
        }
    }

    private fun callAppropriateMerkleTreeFunction(componentGroupSize: Int?, fileContent: String): String {
        return if (componentGroupSize != null) {
            when {
                // This condition is executed when there is no element in the component group.
                // The return value is allOnesHash
                componentGroupSize == 0 -> {
                    fileContent.replace(
                        "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
                        """
        // Return all ones hash
        [true; NODE_DIGEST_BITS]
        """
                    ).replace("\${GROUP_SIZE_PLACEHOLDER}", "2")
                }
                // This condition is executed when the defined group size is an exact power of 2.
                // The return value is the merkle tree function that corresponds to the group size.
                componentGroupSize % 2 == 0 -> {
                    fileContent.replace(
                        "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
                        """
        let component_leaf_hashes = compute_leaf_hashes(this, privacy_salt);

        get_merkle_tree_from_${componentGroupSize}_component_group_leaf_digests(component_leaf_hashes)
        """
                    ).replace("\${GROUP_SIZE_PLACEHOLDER}", componentGroupSize.toString())
                }
                // This condition is executed when the defined group size is not a power of 2.
                // The function finds the next power of 2 and adds padded values to the group.
                // The return value is the merkle tree function that corresponds to the padded group size.
                else -> {
                    val paddedGroupSize = getNextPowerOfTwo(componentGroupSize)
                    fileContent.replace(
                        "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
                        """
        let component_leaf_hashes = compute_leaf_hashes(this, privacy_salt);

        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; $paddedGroupSize];
        for i in 0..$componentGroupSize {
            padded_leaves[i] = component_leaf_hashes[i];
        }

        get_merkle_tree_from_${paddedGroupSize}_component_group_leaf_digests(padded_leaves)
        """
                    ).replace("\${GROUP_SIZE_PLACEHOLDER}", paddedGroupSize.toString())
                }
            }
        } else {
            // This condition is executed when there is no component group size defined.
            // It is possible for notary, timeWindow, parameters groups
            // In that case, we call Merkle tree function for 2 with padded leaves
            fileContent.replace(
                "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
                """
        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; 2];
        padded_leaves[0] = component_leaf_hash;

        get_merkle_tree_from_2_component_group_leaf_digests(padded_leaves)
        """
            ).replace("\${GROUP_SIZE_PLACEHOLDER}", "2")
        }
    }

    private fun getNextPowerOfTwo(value: Int): Int {
        val highestOneBit = Integer.highestOneBit(value)
        return if (value == 1) {
            2
        } else {
            highestOneBit shl 1
        }
    }

    /**
     * Find appropriate merkle tree function for the main merkle tree based on the total component group size
     **/
    private fun findCorrespondingMerkleTreeFunctionForMainTree() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val file = File("${extension.mergedCircuitOutputPath.resolve(circuitName)}/").walk().find { it.name.contains("platform_merkle_tree.zn") }

            if (file != null) {
                val fileContent = file.readText()
                file.delete()
                file.createNewFile()

                val constsTemplate = File("${extension.circuitSourcesBasePath.resolve(circuitName)}/consts.zn").readText()

                val updatedFileContent = fileContent.replace("\${GROUP_SIZE_PLACEHOLDER}", getFullMerkleTreeSize(constsTemplate).toString())
                file.appendBytes(updatedFileContent.toByteArray())
            }
        }
    }
}
