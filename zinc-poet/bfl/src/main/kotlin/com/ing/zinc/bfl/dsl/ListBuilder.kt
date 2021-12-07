package com.ing.zinc.bfl.dsl

import com.ing.zinc.bfl.BflList
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType

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

        fun utf8String(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.I16
            name = "Utf8String$maxSize"
            sizeType = BflPrimitive.U16
        }

        fun asciiString(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.I8
            name = "AsciiString$maxSize"
            sizeType = BflPrimitive.U16
        }

        fun byteArray(maxSize: Int): BflList = list {
            capacity = maxSize
            elementType = BflPrimitive.I8
            name = "ByteArray$maxSize"
        }
    }
}
