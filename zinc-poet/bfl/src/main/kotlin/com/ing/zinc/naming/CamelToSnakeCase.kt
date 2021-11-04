package com.ing.zinc.naming

import com.ing.zinc.bfl.BflPrimitive.Companion.isPrimiviteIdentifier
import java.util.Locale

/**
 * Convert a camelCase string to snake_case.
 */
fun String.camelToSnakeCase(): String {
    return splitCamelParts(this)
        .filterNot { it.codePointType == CodePointType.UNDERSCORE }
        // merge parts that would make a valid primitive identifier, e.g. "U" & "16" -> u16, "I" & "32" -> i32
        .fold(Pair(mutableListOf(), Part.empty())) { acc: Pair<MutableList<Part>, Part>, part: Part ->
            acc.first.add(checkAndCombinePrimitiveTypes(acc, part))
            Pair(acc.first, part)
        }.first
        .joinToString("_") { it.part }
        .toLowerCase(Locale.getDefault())
}

private fun checkAndCombinePrimitiveTypes(
    acc: Pair<MutableList<Part>, Part>,
    part: Part
): Part = if (isPrimiviteIdentifier(acc.second.part.toLowerCase(Locale.getDefault()) + part.part)) {
    combinePrimitiveTypes(acc, part)
} else part

private fun combinePrimitiveTypes(
    acc: Pair<MutableList<Part>, Part>,
    part: Part
): Part {
    val last = acc.first.removeLast()
    return Part(last.part + part.part, part.codePointType)
}

enum class CodePointType {
    UPPER_CASE, LOWER_CASE, UNDERSCORE, OTHER;

    companion object {
        fun fromCodePoint(codePoint: Int): CodePointType {
            return when {
                Character.isLowerCase(codePoint) -> LOWER_CASE
                Character.isUpperCase(codePoint) -> UPPER_CASE
                codePoint == '_'.toInt() -> UNDERSCORE
                else -> OTHER
            }
        }
    }
}

private data class Part(
    val part: String,
    val codePointType: CodePointType
) {
    fun getLastCodePoint() = part.codePoints().reduce { _, b -> b }.orElseThrow {
        IllegalStateException("Empty parts are not supported.")
    }

    fun removeLastCharacter() = Part(
        part.substring(0, part.length - 1),
        codePointType
    )

    companion object {
        fun empty(): Part = Part("", CodePointType.OTHER)
    }
}

/**
 * Helper class to split a camelCase string into multiple [Part]s.
 */
private class PartBuilder {
    /**
     * The codePoint type of this part.
     */
    private var codePointType: CodePointType? = null

    /**
     * The current size of this part.
     */
    private var size = 0

    /**
     * The part.
     */
    private var stringBuilder = StringBuilder()

    /**
     * Resets this builder.
     */
    fun reset() {
        codePointType = null
        size = 0
        stringBuilder = StringBuilder()
    }

    /**
     * Checks whether [codePoint] can be added to this part.
     * Normally codePoints can only be added to a part of the same type, however a LOWER_CASE codePoint can be added
     * to an UPPER_CASE part when the size is 1. In that case the type of the part will be converted to LOWER_CASE.
     */
    fun isCompatible(codePoint: Int): Boolean {
        return when (codePointType) {
            CodePointType.UPPER_CASE -> when (CodePointType.fromCodePoint(codePoint)) {
                CodePointType.UPPER_CASE -> true
                CodePointType.LOWER_CASE -> size == 1
                else -> false
            }
            null -> true
            else -> CodePointType.fromCodePoint(codePoint) == codePointType
        }
    }

    /**
     * Add [codePoint] to this part.
     */
    fun add(codePoint: Int) {
        require(isCompatible(codePoint)) {
            "Cannot add codePoint $codePoint to part with type $codePointType."
        }
        codePointType = CodePointType.fromCodePoint(codePoint)
        stringBuilder.appendCodePoint(codePoint)
        size += 1
    }

    /**
     * Build the actual [Part].
     */
    fun toPart(): Part {
        require(size > 0) {
            "Cannot generate a string from an empty part."
        }
        return Part(stringBuilder.toString(), codePointType!!)
    }
}

/**
 * Split [input] in multiple parts, according to camelCase conventions.
 */
private fun splitCamelParts(input: String): List<Part> {
    // Collect parts
    val parts = mutableListOf<Part>()
    val partBuilder = PartBuilder()
    for (codePoint in input.codePoints()) {
        if (!partBuilder.isCompatible(codePoint)) {
            val part = partBuilder.toPart()
            partBuilder.reset()
            // When going from UPPER_CASE to LOWER_CASE take the last character of the UPPER_CASE part as the first
            // character of the next part.
            if (part.codePointType == CodePointType.UPPER_CASE &&
                CodePointType.fromCodePoint(codePoint) == CodePointType.LOWER_CASE
            ) {
                partBuilder.add(part.getLastCodePoint())
                parts.add(part.removeLastCharacter())
            } else {
                parts.add(part)
            }
        }
        partBuilder.add(codePoint)
    }
    parts.add(partBuilder.toPart())
    // Construct the output
    return parts
}
