package com.ing.zkflow.serialization.utils.binary

import java.nio.ByteBuffer

// ========== Short
fun Short.Companion.representationLength(representation: Representation) = when (representation) {
    Representation.BITS -> SIZE_BITS
    Representation.BYTES -> SIZE_BYTES
}

fun Short.binary(representation: Representation) = when (representation) {
    Representation.BITS -> bits
    Representation.BYTES -> bytes
}

fun ByteArray.short(representation: Representation) = when (representation) {
    Representation.BITS -> bitsToShort()
    Representation.BYTES -> bytesToShort()
}

private val Short.bits: ByteArray
    get() = Integer
        .toBinaryString(this.toInt())
        .padStart(Short.SIZE_BITS, '0')
        .toCharArray()
        .map { "$it".toByte() }
        .toByteArray()
private val Short.bytes: ByteArray
    get() = ByteBuffer.allocate(Short.SIZE_BYTES)
        .putShort(this)
        .array()

private fun ByteArray.bitsToShort(): Short {
    require(size == Short.SIZE_BITS) { "To represent a Short, ByteArray must be ${Short.SIZE_BITS} bits long" }
    return joinToString(separator = "").toInt(2).toShort()
}

private fun ByteArray.bytesToShort(): Short {
    require(size == Short.SIZE_BYTES) { "To represent a Short, ByteArray must be ${Byte.SIZE_BYTES} bytes long" }
    return ByteBuffer.wrap(this).short
}
