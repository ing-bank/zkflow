package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsUnsigned(
    val byte: Byte = -1,
    val ubyte: UByte = 1U,
    val short: Short = -1,
    val ushort: UShort = 1U,
    val int: Int = -1,
    val uint: UInt = 1U,
    val long: Long = -1,
    val ulong: ULong = 1U,
)
