package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflList
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType

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
        sizeType ?: BflPrimitive.U32
    )

    companion object {
        fun list(init: ListBuilder.() -> Unit): BflList = ListBuilder().apply(init).build()

        /**
         * A string is `just` a ByteArray, regardless of the encoding.
         */
        fun string(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.I8
            name = "String$maxSize"
            sizeType = BflPrimitive.U32
        }

        fun byteArray(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.I8
            name = "ByteArray$maxSize"
        }
    }
}
