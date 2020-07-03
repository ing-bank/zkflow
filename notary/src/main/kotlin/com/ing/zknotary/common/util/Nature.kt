package com.ing.zknotary.common.util

sealed class Nature<T> {
    abstract val quality: Int
    abstract val content: T

    data class Authentic<T>(override val content: T) : Nature<T>() {
        override val quality: Int
            get() = 1
    }

    data class Bogus<T>(override val content: T) : Nature<T>() {
        override val quality: Int
            get() = 0
    }
}
