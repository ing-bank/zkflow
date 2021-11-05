package com.ing.zkflow.serialization.utils.binary

fun Boolean.Companion.representationLength(representation: Representation) = when (representation) {
    Representation.BITS -> 1
    Representation.BYTES -> 1
}

fun Boolean.binary(representation: Representation) = when (representation) {
    Representation.BITS -> bits
    Representation.BYTES -> bytes
}

fun ByteArray.boolean(representation: Representation): Boolean = when (representation) {
    Representation.BITS -> bitsToBoolean()
    Representation.BYTES -> bytesToBoolean()
}

private val Boolean.bits: ByteArray
    get() = byteArrayOf(if (this) 1 else 0)
private val Boolean.bytes: ByteArray
    get() = byteArrayOf(if (this) 1 else 0)

fun ByteArray.bitsToBoolean(): Boolean {
    val reqSize = Boolean.representationLength(Representation.BITS)
    require(size == reqSize) { "To represent a Boolean, ByteArray must be $reqSize bits long" }
    return last() == 1.toByte()
}

fun ByteArray.bytesToBoolean(): Boolean {
    val reqSize = Boolean.representationLength(Representation.BYTES)
    require(size == reqSize) { "To represent a Boolean, ByteArray must be $reqSize bytes long" }
    return last() == 1.toByte()
}
