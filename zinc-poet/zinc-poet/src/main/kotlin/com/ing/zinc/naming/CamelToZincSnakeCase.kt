package com.ing.zinc.naming

import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.util.Part
import com.ing.zkflow.util.camelToSnakeCase
import java.util.Locale

/**
 * Convert a camelCase string to snake_case compatible with Zinc.
 */
fun String.camelToZincSnakeCase(): String = camelToSnakeCase(listOf(::toZincCompatibleParts))

fun toZincCompatibleParts(original: List<Part>): List<Part> =
    original.fold(Pair(mutableListOf(), Part.empty())) { acc: Pair<MutableList<Part>, Part>, part: Part ->
        acc.first.add(checkAndCombinePrimitiveZincTypes(acc, part))
        Pair(acc.first, part)
    }.first

// merge parts that would make a valid primitive identifier, e.g. "U" & "16" -> u16, "I" & "32" -> i32
private fun checkAndCombinePrimitiveZincTypes(
    acc: Pair<MutableList<Part>, Part>,
    part: Part
): Part = if (ZincPrimitive.isPrimitiveIdentifier(acc.second.part.toLowerCase(Locale.getDefault()) + part.part)) {
    combinePrimitiveTypes(acc, part)
} else part

private fun combinePrimitiveTypes(
    acc: Pair<MutableList<Part>, Part>,
    part: Part
): Part {
    val last = acc.first.removeLast()
    return Part(last.part + part.part, part.codePointType)
}
