package com.ing.zinc.bfl

import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enumOf
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.string
import com.ing.zinc.bfl.dsl.MapBuilder.Companion.map
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zkflow.zinc.types.asZincJsonObjectList
import com.ing.zkflow.zinc.types.asZincJsonString
import com.ing.zkflow.zinc.types.jsonArrayOf
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
    valueType = string(2)
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
