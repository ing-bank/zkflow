package com.ing.zkflow.serialization.utils

@Suppress("FunctionName")
internal fun WILL_NOT_IMPLEMENT(reason: String): Nothing =
    throw NotImplementedError("An operation IS NOT and WILL NOT be implemented: $reason")
