package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflList
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType

@Suppress("MemberVisibilityCanBePrivate")
@BflDslMarker
class ListBuilder {
    var capacity: Int? = null
    var elementType: BflType? = null
    var name: String? = null
    var sizeType: BflPrimitive? = null

    fun build() = BflList(
        requireNotNull(capacity) { "List property capacity is missing" },
        requireNotNull(elementType) { "List property elementType is missing" },
        name ?: "${elementType!!.typeName()}List$capacity",
        sizeType ?: BflPrimitive.I32
    )

    companion object {
        fun list(init: ListBuilder.() -> Unit): BflList = ListBuilder().apply(init).build()

        /**
         * A string is `just` a ByteArray, regardless of the encoding.
         */
        fun string(maxSize: Int, encodingPrefix: String = "Utf8"): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.I8
            name = "${encodingPrefix}String$maxSize"
        }

        fun byteArray(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.I8
            name = "ByteArray$maxSize"
        }
    }
}
