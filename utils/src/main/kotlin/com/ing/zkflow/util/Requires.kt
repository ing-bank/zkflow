package com.ing.zkflow.util

/**
 * Verifies whether `this` value is not `null`, otherwise throws an [IllegalArgumentException].
 * @receiver Value to verify
 * @param T Type of value
 * @return Non-null value
 */
inline fun <T : Any> T?.requireNotNull(message: () -> String): T = when (this) {
    null -> throw IllegalArgumentException(message())
    else -> this
}

/**
 * Verifies whether `this` collection is not empty, otherwise throws an [IllegalArgumentException].
 * @receiver Collection to verify
 * @param T Type contained in Collection
 * @return Non-empty collection
 */
inline fun <T : Any> Collection<T>.requireNotEmpty(message: () -> String) = ifEmpty {
    throw IllegalArgumentException(message())
}

/**
 * Verifies whether `this` value is an instance of [T], otherwise throws an [IllegalArgumentException].
 * @receiver Value to verify
 * @param T Required type
 * @return Instance of T
 */
inline fun <reified T : Any> Any.requireInstanceOf(message: () -> String) = if (T::class.isInstance(this)) {
    this as T
} else {
    throw IllegalArgumentException(message())
}
