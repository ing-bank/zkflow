package com.ing.zkflow.compilation.zinc.util

import net.corda.core.contracts.ComponentGroupEnum

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

fun getPaddedGroupCount(): Int {
    // Now we assume that we always have NETWORK_PARAMETERS group, so this is pretty much constant,
    // but later this can change. Theoretically we should calculate it in a way similar to Corda:
    // this should be highest enum ordinal value amongst groups expected to be present (padded to power of 2)
    return getNextPowerOfTwo(ComponentGroupEnum.PARAMETERS_GROUP.ordinal)
}

fun getFullMerkleTreeSize(consts: String): Int {
    val search = "GROUP_SIZE: u16 = (\\d+);".toRegex()
    var total = 0
    search.findAll(consts).forEach {
        val groupSize = it.groupValues[1].toInt()
        total += if (groupSize != 0) groupSize else 1 // because if group is empty we use allOnes hash
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
