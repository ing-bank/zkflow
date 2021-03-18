package com.ing.zknotary.gradle.util

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
