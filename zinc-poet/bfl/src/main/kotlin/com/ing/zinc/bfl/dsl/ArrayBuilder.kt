package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflArray
import com.ing.zinc.bfl.BflType

@BflDslMarker
class ArrayBuilder {
    var capacity: Int? = null
    var elementType: BflType? = null

    fun build() = BflArray(
        requireNotNull(capacity) { "Array property capacity is missing" },
        requireNotNull(elementType) { "Array property elementType is missing" }
    )

    companion object {
        fun array(init: ArrayBuilder.() -> Unit): BflArray = ArrayBuilder().apply(init).build()
    }
}
