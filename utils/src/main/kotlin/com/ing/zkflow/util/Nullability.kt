package com.ing.zkflow.util

/**
 * If [predicate] is true, return the result of [ifClause], otherwise return null.
 */
fun <T> ifOrNull(predicate: Boolean, ifClause: () -> T): T? = if (predicate) {
    ifClause()
} else {
    null
}

/**
 * Execute [stmt] when [predicate] is true, otherwise return [this].
 */
fun <T> T.guard(predicate: Boolean, stmt: T.() -> Unit): T = if (predicate) {
    apply(stmt)
} else {
    this
}
