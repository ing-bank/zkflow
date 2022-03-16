package com.ing.zkflow.serialization.test

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

fun <T> List<T>.extendTo(size: Int, default: T): List<T> {
    require(this.size <= size) {
        "List size (${this.size}) is larger than requested size ($size)."
    }
    return this + List(size - this.size) {
        default
    }
}

fun <T : JsonElement> List<T>.toJsonList(size: Int, default: T): JsonObject = buildJsonObject {
    put("size", JsonPrimitive("${this@toJsonList.size}"))
    put("values", JsonArray(extendTo(size, default)))
}
