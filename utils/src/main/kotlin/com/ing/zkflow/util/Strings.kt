package com.ing.zkflow.util

import java.util.Locale

/**
 * Convert a camelCase string to snake_case compatible with Zinc.
 */
fun String.camelToSnakeCase(additionalOperations: List<(List<Part>) -> List<Part>> = emptyList()): String {
    val parts = splitCamelParts(this)
        .filterNot { it.codePointType == CodePointType.UNDERSCORE }

    val processedParts = additionalOperations.fold(parts) { acc, operation ->
        operation(acc)
    }

    return processedParts
        .joinToString("_") { it.part }
        .toLowerCase(Locale.getDefault())
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

data class Part(
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
    fun build(): Part {
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
            val part = partBuilder.build()
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
    parts.add(partBuilder.build())
    // Construct the output
    return parts
}

/**
 * Convert a snake_case string to camelCase.
 *
 * @param capitalize capitalize the first element in the output
 */
fun String.snakeToCamelCase(capitalize: Boolean): String = snakeToCamelCase(capitalize) { listOf(it) }

/**
 * Convert a snake_case string to camelCase.
 *
 * @param capitalize capitalize the first element in the output
 * @param transform function to apply to the individual elements, allowing to further split each element
 */
fun String.snakeToCamelCase(capitalize: Boolean, transform: (String) -> Iterable<String>): String = split("_", "-")
    .flatMap { transform(it) }
    .mapIndexed { index, part ->
        if (part.isEmpty()) {
            part
        } else {
            val head = part[0].let {
                if (capitalize || index > 0) {
                    it.toUpperCase()
                } else {
                    it.toLowerCase()
                }
            }
            val tail = part.substring(1).toLowerCase()
            head + tail
        }
    }
    .joinToString("") { it }

/**
 * Splits a string into a list of strings, splitting before [markers].
 * Compared to [String.split], this function does not remove the markers from the result.
 */
fun String.splitBefore(vararg markers: String): List<String> {
    val result = mutableListOf<String>()
    var currentIndex = 0
    do {
        val index = markers
            .map { marker -> this.indexOf(marker, startIndex = currentIndex + 1) }
            .filter { it >= currentIndex }
            .minOrNull()
        currentIndex = if (index == null) {
            result.add(this.substring(currentIndex))
            -1
        } else {
            result.add(this.substring(currentIndex, index))
            index
        }
    } while (currentIndex > 0)
    return result.toList()
}
