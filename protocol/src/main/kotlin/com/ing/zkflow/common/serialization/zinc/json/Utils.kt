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

const val BYTE_BITS = 8
fun Int.toBits(): List<JsonElement> {
    val bits = MutableList(BYTE_BITS) { JsonPrimitive(false) }
    for (index in 0..BYTE_BITS) {
        if ((this.shr(index) and 1) == 1) {
            bits[BYTE_BITS - 1 - index] = JsonPrimitive(true)
        }
    }
    return bits
}
