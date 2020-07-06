package com.ing.zknotary.common.util

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * This class wraps a value of any type to mark contained data
 * as either Authentic or Bogus.
 */
sealed class Nature<T> {
    // see https://github.com/FasterXML/jackson-module-kotlin/issues/80
    @get:JsonProperty("isBogus")
    abstract val isBogus: Int

    abstract val content: T

    data class Authentic<T>(override val content: T) : Nature<T>() {
        override val isBogus: Int
            get() = 1
    }

    data class Bogus<T>(override val content: T) : Nature<T>() {
        override val isBogus: Int
            get() = 0
    }
}
