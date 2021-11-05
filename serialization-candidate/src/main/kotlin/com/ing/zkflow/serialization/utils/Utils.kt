package com.ing.zkflow.serialization.utils

@Suppress("MagicNumber")
internal val Char.isASCII: Boolean
    get() = toInt() in 0..255

@Suppress("FunctionName")
internal fun WILL_NOT_IMPLEMENT(reason: String): Nothing =
    throw NotImplementedError("An operation IS NOT and WILL NOT be implemented: $reason")
