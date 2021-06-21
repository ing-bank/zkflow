package io.ivno.collateraltoken.zinc.types

import com.ing.zknotary.zinc.types.optional
import io.onixlabs.corda.bnms.contract.Setting
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.addJsonArray

fun Set<Setting<String>>.toJsonArray(size: Int, valueLength: Int): JsonArray = buildJsonArray {
    map { add(it.toJsonObject(valueLength).optional()) }
    addJsonArray {
        repeat(size-this@toJsonArray.size) {

        }
    }
}

fun Set<AbstractClaim<String>>.toJsonArray(size: Int, propertyLength: Int, valueLength: Int): JsonArray = buildJsonArray {
    map { add(it.toJsonObject(propertyLength, valueLength).optional()) }
    addJsonArray {
        repeat(size-this@toJsonArray.size) {

        }
    }
}