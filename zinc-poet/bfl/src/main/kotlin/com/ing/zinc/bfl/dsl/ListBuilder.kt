package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflList
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType

class ListBuilder {
    var capacity: Int? = null
    var elementType: BflType? = null
    var id: String? = null
    var sizeType: BflPrimitive? = null

    fun build() = BflList(
        requireNotNull(capacity) { "List property capacity is missing" },
        requireNotNull(elementType) { "List property elementType is missing" },
        id,
        sizeType ?: BflPrimitive.U32
    )

    companion object {
        fun list(init: ListBuilder.() -> Unit): BflList = ListBuilder().apply(init).build()

        fun utfString(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.U16
            id = "String$capacity"
            sizeType = BflPrimitive.U16
        }

        fun asciiString(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.U8
            id = "String$capacity"
            sizeType = BflPrimitive.U16
        }

        fun byteArray(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.U8
            id = "ByteArray$maxSize"
        }
    }
}
