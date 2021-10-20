package com.ing.zkflow.compilation.zinc.util

fun getNextPowerOfTwo(value: Int): Int {
    val highestOneBit = Integer.highestOneBit(value)
    return if (value == 1) {
        2
    } else {
        highestOneBit shl 1
    }
}

fun isPowerOfTwo(value: Int): Boolean {
    return value > 0 && (value and (value - 1)) == 0
}
