package com.ing.zkflow.serialization.test

import com.ing.zkflow.util.extendTo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

fun <T : JsonElement> List<T>.toJsonList(size: Int, default: T): JsonObject = buildJsonObject {
    put("size", JsonPrimitive("${this@toJsonList.size}"))
    put("values", JsonArray(extendTo(size, default)))
}
