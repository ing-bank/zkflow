@file:Suppress("TooManyFunctions")

package com.ing.zkflow.zinc.types

import com.ing.zinc.bfl.BflBigDecimal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal

// JSON utilities
public fun String.parseJson(): JsonElement = Json.parseToJsonElement(this)

public fun String.asZincJsonString(size: Int): JsonObject = buildJsonObject {
    require(size >= this@asZincJsonString.length) { "String does not fit in given size" }
    put("size", "${this@asZincJsonString.length}")
    put(
        "values",
        buildJsonArray {
            codePoints().forEach {
                add("$it")
            }
            (this@asZincJsonString.length until size).forEach { _ ->
                add("0")
            }
        }
    )
}

public fun zincJsonOptionOf(jsonObject: JsonElement, isPresent: Boolean = true): JsonObject = buildJsonObject {
    put("has_value", isPresent)
    put("value", jsonObject)
}

public fun Collection<JsonElement>.asZincJsonObjectList(size: Int, emptyValue: JsonElement): JsonObject = buildJsonObject {
    require(size >= this@asZincJsonObjectList.size) { "Collection does not fit in given size" }
    put("size", "${this@asZincJsonObjectList.size}")
    put(
        "values",
        buildJsonArray {
            forEach {
                add(it)
            }
            (this@asZincJsonObjectList.size until size).forEach { _ ->
                add(emptyValue)
            }
        }
    )
}

public fun Collection<Number>.asZincJsonNumberList(size: Int, paddingValue: Int = 0, fullCapacity: Boolean = false): JsonObject = buildJsonObject {
    require(size >= this@asZincJsonNumberList.size) { "Collection does not fit in given size" }
    put("size", "${if (fullCapacity) size else this@asZincJsonNumberList.size}")
    put(
        "values",
        buildJsonArray {
            forEach {
                add("$it")
            }
            (this@asZincJsonNumberList.size until size).forEach { _ ->
                add("$paddingValue")
            }
        }
    )
}

public fun Collection<Boolean>.asZincJsonBooleanList(size: Int, paddingValue: Boolean = false): JsonObject = buildJsonObject {
    require(size >= this@asZincJsonBooleanList.size) { "Collection does not fit in given size" }
    put("size", "${this@asZincJsonBooleanList.size}")
    put(
        "values",
        buildJsonArray {
            forEach {
                add(it)
            }
            (this@asZincJsonBooleanList.size until size).forEach { _ ->
                add(paddingValue)
            }
        }
    )
}

public fun jsonArrayOf(vararg numbers: Number): JsonArray = buildJsonArray {
    numbers.forEach {
        add("$it")
    }
}

public fun jsonArrayOf(vararg bools: Boolean): JsonArray = buildJsonArray {
    bools.forEach {
        add(it)
    }
}

public fun BigDecimal.toJsonObject(module: BflBigDecimal): JsonObject = toJsonObject(module.integerSize, module.fractionSize)
public fun BigDecimal.toJsonObject(integerSize: Int = 24, fractionSize: Int = 6, kind: Int = 3): JsonObject = buildJsonObject {
    val integer = IntArray(integerSize)
    val fraction = IntArray(fractionSize)

    this@toJsonObject.let {
        val stringRepresentation = toPlainString()
        val integerFractionTuple = stringRepresentation.removePrefix("-").split(".")

        integerFractionTuple[0].reversed().forEachIndexed { idx, char ->
            integer[idx] = Character.getNumericValue(char)
        }

        if (integerFractionTuple.size == 2) {
            integerFractionTuple[1].forEachIndexed { idx, char ->
                fraction[idx] = Character.getNumericValue(char)
            }
        }
    }
    put("kind", JsonPrimitive("$kind"))
    put("sign", "${this@toJsonObject.signum()}")
    put("integer", integer.toList().asZincJsonNumberList(integerSize, fullCapacity = true))
    put("fraction", fraction.toList().asZincJsonNumberList(fractionSize, fullCapacity = true))
}
