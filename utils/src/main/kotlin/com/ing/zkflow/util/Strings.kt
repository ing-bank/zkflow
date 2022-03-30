package com.ing.zkflow.util

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
