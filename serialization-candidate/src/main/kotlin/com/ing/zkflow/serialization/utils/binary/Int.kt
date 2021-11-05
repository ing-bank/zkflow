package com.ing.zkflow.serialization.utils.binary

import java.nio.ByteBuffer

fun Int.Companion.representationLength(representation: Representation) = when (representation) {
    Representation.BITS -> SIZE_BITS
    Representation.BYTES -> SIZE_BYTES
}

fun Int.binary(representation: Representation) = when (representation) {
    Representation.BITS -> bits
    Representation.BYTES -> bytes
}

fun ByteArray.int(representation: Representation) = when (representation) {
    Representation.BITS -> bitsToInt()
    Representation.BYTES -> bytesToInt()
}

private val Int.bits: ByteArray
    get() = Integer
        .toBinaryString(this)
        .padStart(Int.SIZE_BITS, '0')
        .toCharArray()
        .map { "$it".toByte() }
        .toByteArray()

private val Int.bytes: ByteArray
    get() = ByteBuffer.allocate(Int.SIZE_BYTES)
        .putInt(this)
        .array()

private fun ByteArray.bitsToInt(): Int {
    require(size == Int.SIZE_BITS) { "To represent an Int, ByteArray must be ${Int.SIZE_BITS} bits long" }
    return joinToString(separator = "").toLong(2).toInt()
}

private fun ByteArray.bytesToInt(): Int {
    require(size == Int.SIZE_BYTES) { "To represent an Int, ByteArray must be ${Int.SIZE_BYTES} bytes long" }
    return ByteBuffer.wrap(this).int
}
