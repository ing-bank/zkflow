package io.ivno.collateraltoken.zinc.types

import com.ing.zknotary.testing.toJsonArray
import com.ing.zknotary.testing.toSizedIntArray
import io.ivno.collateraltoken.serialization.RoleSurrogate
import io.onixlabs.corda.bnms.contract.Role
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Role.toZincJson() = toJsonObject().toString()

fun String?.toJsonObject(size: Int) = buildJsonObject {
    put("chars", toSizedIntArray(size).toJsonArray())
    put("size", "${this@toJsonObject?.length ?: 0}")
}

fun Role.toJsonObject() = buildJsonObject {
    put("value", value.toJsonObject(RoleSurrogate.VALUE_LENGTH))
}