package com.ing.zknotary.common.util

fun Byte.asUnsigned() = this.toInt() and 0xFF
