package com.ing.zknotary.gradle.util

import java.io.File

class MerkleReplacer(private val outputPath: File) {

    /**
     * Find appropriate merkle tree function for the main merkle tree based on the total component group size
     **/
    fun setCorrespondingMerkleTreeFunctionForMainTree() {

        val targetFiles = createOutputFile(outputPath)
        val file = targetFiles.walk()
            .find { it.name.contains("platform_merkle_tree.zn") }

        if (file != null) {
            val fileContent = file.readText()
            file.delete()
            file.createNewFile()

            val updatedFileContent =
                fileContent.replace("\${GROUP_SIZE_PLACEHOLDER}", getPaddedGroupCount().toString())
            file.writeBytes(updatedFileContent.toByteArray())
        }
    }

    /**
     * Find appropriate merkle tree functions for each component group subtree
     * based on component group size
     **/
    fun setCorrespondingMerkleTreeFunctionForComponentGroups(constsContent: String) {

        val targetFiles = createOutputFile(outputPath)

        val componentGroupFiles = targetFiles.walk().filter {
            it.name.contains("platform_components")
        }
        componentGroupFiles.forEach { file ->
            // Filter component group files

            val fileContent = file.readText()
            // Find which component group
            // TODO won't it be easier to just check file name? way shorter, more stable and we use already to find component group files
            val componentRegex = "Serialized(\\w+)(Group)".toRegex()
            var componentGroupName = componentRegex.find(fileContent)?.groupValues?.get(1)

            if (componentGroupName != "Parameters" && // TODO _probably_ we need more formal definition
                componentGroupName?.endsWith("s")!!
            ) componentGroupName = componentGroupName.dropLast(1)

            val componentGroupSize = getMerkleTreeSizeForComponent(componentGroupName, constsContent)

            // Find the size of the component group in consts.zn
            file.delete()
            file.createNewFile()
            val updatedFileContent = replaceAppropriateMerkleTreeFunction(componentGroupSize, fileContent)

            file.writeBytes(updatedFileContent.toByteArray())
        }
    }

    private fun replaceAppropriateMerkleTreeFunction(componentGroupSize: Int?, fileContent: String): String {
        return when {
            componentGroupSize == null -> {
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

            // This condition is executed when there is no element in the component group.
            // The return value is allOnesHash
            // TODO this can also be allZeroes if group's enum ordinal is higher than highest existing,
            //  but in practice this will never happen as soon as we have NETWORK_PARAMETERS
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
            // 1 doesn't count because Corda expects at least 2 leaves so 1 should be padded TODO confirm
            // The return value is the merkle tree function that corresponds to the group size.
            isPowerOfTwo(componentGroupSize) && componentGroupSize != 1 -> {
                fileContent.replace(
                    "// ### CALL APPROPRIATE MERKLE TREE FUNCTION ###",
                    """
        let component_leaf_hashes = compute_leaf_hashes(components, privacy_salt);

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
        let component_leaf_hashes = compute_leaf_hashes(components, privacy_salt);

        let mut padded_leaves = [[false; COMPONENT_GROUP_LEAF_DIGEST_BITS]; $paddedGroupSize];
        for i in 0..$componentGroupSize {
            padded_leaves[i] = component_leaf_hashes[i];
        }

        get_merkle_tree_from_${paddedGroupSize}_component_group_leaf_digests(padded_leaves)
        """
                ).replace("\${GROUP_SIZE_PLACEHOLDER}", paddedGroupSize.toString())
            }
        }
    }
}
