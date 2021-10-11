package com.ing.zkflow.util

private val camelRegex = "(?<=[a-z])[A-Z]".toRegex()

fun String.camelToSnakeCase(): String {
    return camelRegex.replace(this.replace("EdDSA", "EdDsa")) {
        "_${it.value}"
    }
        .removeSuffix("_")
        .removePrefix("_")
        .toLowerCase()
}

fun String.snakeCaseToCamel(): String = this
    .split("_")
    .joinToString(separator = "") { word -> word.firstCharToUpperCase() }
    .replace("EdDsa", "EdDSA")

fun String.firstCharToUpperCase(): String =
    if (isNotEmpty()) { this[0].toUpperCase() + substring(1) } else this
