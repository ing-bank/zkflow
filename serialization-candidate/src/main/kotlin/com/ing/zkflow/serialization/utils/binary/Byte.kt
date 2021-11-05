package com.ing.zkflow.serialization.utils.binary

// ========== Byte
fun Byte.Companion.representationLength(representation: Representation) = when (representation) {
    Representation.BITS -> SIZE_BITS
    Representation.BYTES -> SIZE_BYTES
}

fun Byte.binary(representation: Representation) = when (representation) {
    Representation.BITS -> bits
    Representation.BYTES -> bytes
}

fun ByteArray.byte(representation: Representation) = when (representation) {
    Representation.BITS -> bitsToByte()
    Representation.BYTES -> single()
}

@Suppress("MagicNumber")
private val Byte.bits: ByteArray
    get() = Integer
        .toBinaryString(this.toInt() and 0xFF)
        .padStart(Byte.SIZE_BITS, '0')
        .toCharArray()
        .map { "$it".toByte() }
        .toByteArray()

private val Byte.bytes: ByteArray
    get() = ByteArray(1) { this }

fun ByteArray.bitsToByte(): Byte {
    require(size == Byte.SIZE_BITS) { "To represent a Byte, ByteArray must be ${Byte.SIZE_BITS} bits long" }
    return joinToString(separator = "").toInt(2).toByte()
}
