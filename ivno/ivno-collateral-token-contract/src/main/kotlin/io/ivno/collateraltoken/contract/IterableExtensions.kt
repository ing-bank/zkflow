package io.ivno.collateraltoken.contract

/**
 * Determines whether an [Iterable] contains distinct elements.
 *
 * @param T The underlying type of the [Iterable].
 * @return Returns true if the [Iterable] contains distinct elements; otherwise, false.
 */
fun <T> Iterable<T>.isDistinct(): Boolean {
    return distinct().size == count()
}
