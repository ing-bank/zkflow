package com.ing.zkflow.util

inline fun tryNonFailing(block: () -> Unit) {
    try {
        block()
    } catch (_: Exception) {
        // Ignore
    }
}
