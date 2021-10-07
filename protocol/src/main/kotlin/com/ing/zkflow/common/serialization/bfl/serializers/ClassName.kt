package com.ing.zkflow.common.serialization.bfl.serializers

import java.nio.charset.StandardCharsets

fun String.toBytes() = this.toByteArray(StandardCharsets.US_ASCII)
fun <T> Class<T>.toBytes() = this.name.toBytes()

fun ByteArray.getOriginalClassname(): String = String(this, StandardCharsets.US_ASCII)
fun ByteArray.getOriginalClass(): Class<*> = Class.forName(getOriginalClassname())
