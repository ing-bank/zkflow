package com.ing.zkflow.util

fun String.snakeToCamelCase(capitalize: Boolean): String {
    return split("_", "-").mapIndexed { index, part ->
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
    }.joinToString("") { it }
}
