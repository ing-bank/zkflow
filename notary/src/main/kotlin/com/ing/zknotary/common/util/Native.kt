package com.ing.zknotary.common.util

import com.sun.jna.Memory
import com.sun.jna.Native

fun ArrayList<Int>.toNative(): Memory {
    val arrayListAsNativeMemory = Memory(this.size.toLong() * Native.getNativeSize(Int::class.javaObjectType))
    this.forEachIndexed { index, element ->
        arrayListAsNativeMemory.setInt(
            index.toLong() * Native.getNativeSize(Int::class.javaObjectType),
            element
        )
    }
    return arrayListAsNativeMemory
}

fun ByteArray.toNative(): Memory {
    val byteArrayAsNativeMemory = Memory(this.size.toLong() * Native.getNativeSize(Byte::class.javaObjectType))
    this.forEachIndexed { index, element ->
        byteArrayAsNativeMemory.setByte(
            index.toLong() * Native.getNativeSize(Byte::class.javaObjectType),
            element
        )
    }
    return byteArrayAsNativeMemory
}
