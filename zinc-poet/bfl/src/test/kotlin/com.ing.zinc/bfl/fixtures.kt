package com.ing.zinc.bfl

import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enumOf
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.utf8String
import com.ing.zinc.bfl.dsl.MapBuilder.Companion.map
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.PolyBuilder.Companion.poly
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class Things {
    NOTHING,
    SOMETHING,
    ANYTHING,
    EVERYTHING
}

sealed class DataInterface {
    data class IntData(val value: Int) : DataInterface()
    data class BoolData(val value: Boolean) : DataInterface()

    companion object {
        val intData = struct {
            name = "IntData"
            field {
                name = "value"
                type = BflPrimitive.I32
            }
        }
        val boolData = struct {
            name = "BoolData"
            field {
                name = "value"
                type = BflPrimitive.Bool
            }
        }
        val polyIntData = poly { innerType = intData }
        val polyBoolData = poly { innerType = boolData }
    }
}

internal val thingsEnum = enumOf(Things::class)

internal val booleanOption = option {
    innerType = BflPrimitive.Bool
}

internal val intOption = option {
    innerType = BflPrimitive.I8
}

internal val structWithPrimitiveFields = struct {
    name = "StructWithPrimitiveFields"
    field {
        name = "foo"
        type = BflPrimitive.U32
    }
    field {
        name = "bar"
        type = BflPrimitive.Bool
    }
}

internal val structWithArrayFieldsOfPrimitives = struct {
    name = "StructWithArrayFields"
    field {
        name = "foo"
        type = array {
            capacity = 2
            elementType = BflPrimitive.U32
        }
    }
    field {
        name = "bar"
        type = array {
            capacity = 2
            elementType = BflPrimitive.Bool
        }
    }
}

internal val structWithStructField = struct {
    name = "StructWithStructField"
    field {
        name = "baz"
        type = structWithPrimitiveFields
    }
}

internal val listOfBools = list {
    capacity = 2
    elementType = BflPrimitive.Bool
}

internal val listOfEnums = list {
    capacity = 2
    elementType = thingsEnum
}

internal val listOfArraysOfU8 = list {
    capacity = 2
    elementType = array {
        capacity = 2
        elementType = BflPrimitive.U8
    }
}

internal val listOfStructWithStructField = list {
    capacity = 2
    elementType = structWithStructField
}

internal val mapOfEnumToString = map {
    capacity = 2
    keyType = thingsEnum
    valueType = utf8String(2)
}

internal val emptyJsonObject = buildJsonObject { }

internal val testDataListOfArrays = listOf(jsonArrayOf(5, 8))
    .asZincJsonObjectList(2, jsonArrayOf(0, 0))

internal val testDataListOfStructWithStructs = listOf(structStructJson(true, 1))
    .asZincJsonObjectList(2, structStructJson(false, 0))

internal val testDataDuplicateListOfStructWithStructs = listOf(
    structStructJson(true, 1),
    structStructJson(true, 1),
)
    .asZincJsonObjectList(2, structStructJson(false, 0))

internal val testDataLargerListOfStructWithStructs = listOf(
    structStructJson(true, 1),
    structStructJson(true, 13)
)
    .asZincJsonObjectList(2, structStructJson(false, 0))

internal val testDataLargerListOfStructs = listOf(
    primitiveStructJson(true, 1),
    primitiveStructJson(true, 13)
)
    .asZincJsonObjectList(2, primitiveStructJson(false, 0))

internal val testDataEmptyListOfStructWithStructs = emptyList<JsonObject>()
    .asZincJsonObjectList(2, structStructJson(false, 0))

// JSON utilities
fun String.parseJson(): JsonElement = Json.parseToJsonElement(this)

fun String.asZincJsonString(size: Int): JsonObject = buildJsonObject {
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

fun zincJsonOptionOf(jsonObject: JsonElement, isPresent: Boolean = true): JsonObject = buildJsonObject {
    put("has_value", isPresent)
    put("value", jsonObject)
}

fun Collection<JsonElement>.asZincJsonObjectList(size: Int, emptyValue: JsonElement): JsonObject = buildJsonObject {
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

fun Collection<Number>.asZincJsonNumberList(size: Int, paddingValue: Int = 0): JsonObject = buildJsonObject {
    require(size >= this@asZincJsonNumberList.size) { "Collection does not fit in given size" }
    put("size", "${this@asZincJsonNumberList.size}")
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

fun Collection<Boolean>.asZincJsonBooleanList(size: Int, paddingValue: Boolean = false): JsonObject = buildJsonObject {
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

fun Map<Things, String>.asZincJsonMap(size: Int): JsonObject = buildJsonObject {
    require(size >= this@asZincJsonMap.size) { "Map does not fit in given size" }
    put("size", "${this@asZincJsonMap.size}")
    put(
        "values",
        buildJsonArray {
            this@asZincJsonMap.forEach {
                add(
                    buildJsonObject {
                        put("key", "${it.key.ordinal}")
                        put("value", it.value.asZincJsonString(2))
                    }
                )
            }
            (this@asZincJsonMap.size until size).forEach { _ ->
                add(
                    buildJsonObject {
                        put("key", "0")
                        put("value", "".asZincJsonString(2))
                    }
                )
            }
        }
    )
}

fun jsonArrayOf(vararg numbers: Number) = buildJsonArray {
    numbers.forEach {
        add("$it")
    }
}

fun jsonArrayOf(vararg bools: Boolean) = buildJsonArray {
    bools.forEach {
        add(it)
    }
}

fun primitiveStructJson(bar: Boolean, foo: Long) = buildJsonObject {
    put("bar", bar)
    put("foo", "$foo")
}

fun arrayStructJson(bar: Collection<Boolean>, foo: Collection<Long>) = buildJsonObject {
    put(
        "bar",
        buildJsonArray {
            for (b in bar) { add(b) }
        }
    )
    put(
        "foo",
        buildJsonArray {
            for (f in foo) { add("$f") }
        }
    )
}

fun structStructJson(bar: Boolean, foo: Long) = buildJsonObject {
    put("baz", primitiveStructJson(bar, foo))
}
