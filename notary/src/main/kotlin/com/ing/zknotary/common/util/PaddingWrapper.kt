package com.ing.zknotary.common.util

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * This class wraps a value of any type to mark contained data
 * as either Authentic or Bogus.
 *
 * This is currently primarily used for serialization purposes
 * so that Zinc will be able to distinguish contents for further analysis,
 * such as verification w.r.t. the contract rules.
 */
sealed class PaddingWrapper<T> {
    // see https://github.com/FasterXML/jackson-module-kotlin/issues/80
    @get:JsonProperty("is_filler")
    abstract val isFiller: Boolean

    abstract val content: T

    data class Original<T>(override val content: T) : PaddingWrapper<T>() {
        override val isFiller = false
    }

    data class Filler<T>(override val content: T) : PaddingWrapper<T>() {
        override val isFiller = true
    }
}
