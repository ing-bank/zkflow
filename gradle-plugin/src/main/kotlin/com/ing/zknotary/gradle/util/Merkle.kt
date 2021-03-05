package com.ing.zknotary.gradle.util

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
