package com.ing.zknotary.common.util

@ExperimentalUnsignedTypes
fun ByteArray.toUIntList(): List<UInt> = map { it.toUInt() }
fun ByteArray.toIntList(): List<Int> = map { it.toInt() }
