package com.ing.zinc

import com.ing.zinc.bfl.BflType.Companion.BITS_PER_BYTE

inline fun <T : Any> requireNotEmpty(list: List<T>, message: () -> String) = list.ifEmpty {
    throw IllegalArgumentException(message())
}

fun Int.toByteBoundary() = ((this - 1) / BITS_PER_BYTE + 1) * BITS_PER_BYTE
