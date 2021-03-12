package com.ing.zknotary.gradle.util

import java.io.File

fun getMerkleTree(templateContents: String, fullMerkleLeaves: Int, digestSnakeCase: String, digestCamelCase: String, digestBits: String): String {
    var digestMerkleFunctions = ""
    // Compute the root
    digestMerkleFunctions +=
        """
fn get_merkle_tree_from_2_$digestSnakeCase(leaves: [${digestCamelCase}Bits; 2]) -> ${digestCamelCase}Bits {
    pedersen_to_padded_bits(pedersen(concatenate_$digestSnakeCase(leaves[0], leaves[1])).0)
}
"""
    if (fullMerkleLeaves > 2) {
        var leaves = 4
        do {
            val levelUp = leaves / 2
            digestMerkleFunctions += templateContents.replace("\${NUM_LEAVES_PLACEHOLDER}", leaves.toString())
                .replace("\${DIGEST_TYPE_PLACEHOLDER}", digestSnakeCase)
                .replace("\${DIGEST_BITS_TYPE_PLACEHOLDER}", "${digestCamelCase}Bits")
                .replace("\${DIGEST_BITS_PLACEHOLDER}", digestBits)
                .replace("\${DTO_PLACEHOLDER}", "${digestCamelCase}Dto")
                .replace("\${LEVEL_UP_PLACEHOLDER}", levelUp.toString())
            leaves *= 2
        } while (leaves <= fullMerkleLeaves)
    }
    return digestMerkleFunctions
}

fun getFullMerkleTreeSize(consts: String): Int {
    val search = "GROUP_SIZE: u16 = (\\d+);".toRegex()
    var total = 3 // notary, timewindow, and parameters group size
    search.findAll(consts).forEach {
        total += it.groupValues[1].toInt()
    }

    fun isPow2(num: Int) = num and (num - 1) == 0
    return run {
        var l = total
        while (!isPow2(l)) {
            l++
        }
        l
    }
}

fun getMerkleTreeSizeForComponent(componentGroupName: String, consts: String): Int? {
    val componentRegex = "${componentGroupName.toUpperCase()}_GROUP_SIZE: u16 = (\\d+);".toRegex()
    return componentRegex.find(consts)?.groupValues?.get(1)?.toInt()
}

/**
 * Find appropriate merkle tree function for the main merkle tree based on the total component group size
 **/
fun setCorrespondingMerkleTreeFunctionForMainTree(targetFilePath: String, constsContent: String) {

    val file = File("$targetFilePath/").walk()
        .find { it.name.contains("platform_merkle_tree.zn") }

    if (file != null) {
        val fileContent = file.readText()
        file.delete()
        file.createNewFile()

        val updatedFileContent =
            fileContent.replace("\${GROUP_SIZE_PLACEHOLDER}", getFullMerkleTreeSize(constsContent).toString())
        file.writeBytes(updatedFileContent.toByteArray())
    }
}

/**
 * Find appropriate merkle tree functions for each component group subtree
 * based on component group size
 **/
fun setCorrespondingMerkleTreeFunctionForComponentGroups(targetFilePath: String, constsContent: String) {
    val componentGroupFiles = File("$targetFilePath/").walk().filter {
        it.name.contains("platform_components")
    }
    componentGroupFiles.forEach { file ->
        // Filter component group files

        val fileContent = file.readText()
        // Find which component group
        val componentRegex = "struct (\\w+)(ComponentGroup)".toRegex()
        var componentGroupName = componentRegex.find(fileContent)?.groupValues?.get(1)

        if (componentGroupName?.endsWith("s")!!) componentGroupName = componentGroupName.dropLast(1)

        val componentGroupSize = getMerkleTreeSizeForComponent(componentGroupName, constsContent)

        // Find the size of the component group in consts.zn
        file.delete()
        file.createNewFile()
        val updatedFileContent = replaceAppropriateMerkleTreeFunction(componentGroupSize, fileContent)

        file.writeBytes(updatedFileContent.toByteArray())
    }
}

private fun replaceAppropriateMerkleTreeFunction(componentGroupSize: Int?, fileContent: String): String {
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
