package com.ing.zkflow.common.serialization.zinc.json

import com.ing.dlt.zkkrypto.util.asUnsigned
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.corda.core.crypto.SecureHash

fun ByteArray.toUnsignedBitString() = map { byte -> byte.asUnsigned().toBits() }.flatten()

@JvmName("toJsonArrayByteArray")
fun List<ByteArray>.toJsonArray() = map { JsonArray(it.toUnsignedBitString()) }
fun List<SecureHash>.toJsonArray() = map { JsonArray(it.bytes.toUnsignedBitString()) }

private fun Int.toBits(): List<JsonElement> {
    return (1..Byte.SIZE_BITS).map {
        val index = Byte.SIZE_BITS - it
        JsonPrimitive((this.shr(index) and 1) == 1)
    }
}
