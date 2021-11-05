package com.ing.zkflow.serialization.utils.binary

import java.nio.ByteBuffer

fun Long.Companion.representationLength(representation: Representation) = when (representation) {
    Representation.BITS -> SIZE_BITS
    Representation.BYTES -> SIZE_BYTES
}

fun Long.binary(representation: Representation) = when (representation) {
    Representation.BITS -> bits
    Representation.BYTES -> bytes
}

fun ByteArray.long(representation: Representation) = when (representation) {
    Representation.BITS -> bitsToLong()
    Representation.BYTES -> bytesToLong()
}

private val Long.bits: ByteArray
    get() = java.lang.Long
        .toBinaryString(this)
        .padStart(Long.SIZE_BITS, '0')
        .toCharArray()
        .map { "$it".toByte() }
        .toByteArray()

private val Long.bytes: ByteArray
    get() = ByteBuffer
        .allocate(Long.SIZE_BYTES)
        .putLong(this)
        .array()

private fun ByteArray.bitsToLong(): Long {
    require(size == Long.SIZE_BITS) { "To represent a Long, ByteArray must be ${Long.SIZE_BITS} bits long" }
    // Usually bit conversion is performed by built-in methods via treating the bit string as it if was a larger type,
    // e.g., Byte is parsed as Int and then converted to Byte, otherwise the built-in methods fails for edge cases.
    // In case of Long, there is no larger numeric type, so it is parsed byte-wise.
    return toList()
        .chunked(Byte.SIZE_BITS) { it.toByteArray().bitsToByte() }
        .toByteArray()
        .bytesToLong()
}

private fun ByteArray.bytesToLong(): Long {
    require(size == Long.SIZE_BYTES) { "To represent an Int, ByteArray must be ${Long.SIZE_BYTES} bytes long" }
    return ByteBuffer.wrap(this).long
}
