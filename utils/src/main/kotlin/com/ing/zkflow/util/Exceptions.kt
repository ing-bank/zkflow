package com.ing.zkflow.util

inline fun tryNonFailing(block: () -> Unit) {
    try {
        block()
    } catch (_: Exception) {
        // Ignore
    }
}

@Suppress("FunctionName")
fun FEATURE_MISSING(reason: String): Nothing = throw NotImplementedError("This feature is not yet implemented: $reason")

@Suppress("FunctionName")
fun STUB_FOR_TESTING(): Nothing = throw NotImplementedError("This is a function stub, and intentionally not implemented. It should not be called")
