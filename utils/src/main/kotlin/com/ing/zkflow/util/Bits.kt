package com.ing.zkflow.util

/**
 * Returns the minimal number of bytes needed to hold this many bits.
 * @receiver Number of bits
 * @return Minimum number of bytes to hold the number of bits
 */
fun Int.bitsToBytes() = (this + Byte.SIZE_BITS - 1) / Byte.SIZE_BITS

/**
 * Returns the number of bits in the minimal bytearray that can hold this many bits.
 * @receiver Number of bits
 * @return Number of bits aligned to byte boundary
 */
fun Int.bitsToByteBoundary() = bitsToBytes() * Byte.SIZE_BITS
