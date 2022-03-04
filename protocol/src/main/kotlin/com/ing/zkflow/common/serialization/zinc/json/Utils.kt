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

fun Int.toBits(): List<JsonElement> {
    val bits = MutableList(Byte.SIZE_BITS) { JsonPrimitive(false) }
    for (index in 0..Byte.SIZE_BITS) {
        if ((this.shr(index) and 1) == 1) {
            bits[Byte.SIZE_BITS - 1 - index] = JsonPrimitive(true)
        }
    }
    return bits
}
