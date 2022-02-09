package com.ing.zkflow.util

/**
 *  If [predicate] is true, return the result of [ifClause], otherwise return null.
 */
fun <T> ifOrNull(predicate: Boolean, ifClause: () -> T): T? = if (predicate) {
    ifClause()
} else {
    null
}
