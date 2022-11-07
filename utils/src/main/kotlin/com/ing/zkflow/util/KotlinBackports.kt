@file:Suppress("MatchingDeclarationName")
package com.ing.zkflow.util

import java.time.Duration
import java.time.Instant
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

data class TimedValue<T>(val value: T, val duration: Duration)

/**
 * Executes the given function [block] and returns an instance of [TimedValue] class, containing both
 * the result of the function execution and the duration of elapsed time interval.
 */
inline fun <T> measureTimedValue(block: () -> T): TimedValue<T> {
    val start = Instant.now()
    val result = block()
    return TimedValue(result, Duration.between(start, Instant.now()))
}

/**
 * Executes the given function [block] and returns the duration of elapsed time interval.
 */
inline fun measureTime(block: () -> Unit): Duration {
    val start = Instant.now()
    block()
    return Duration.between(start, Instant.now())
}

/**
 * Returns the smallest element or `null` if there are no elements.
 *
 * Since we code to Kotlin API 1.3, we copy this impl from the 1.4 stdlib
 */
fun <T : Comparable<T>> Iterable<T>.minOrNull(): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var min = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
inline fun <T, R : Comparable<R>> List<T>.minByOrNull(selector: (T) -> R): T? {
    if (isEmpty()) return null
    var minElem = this[0]
    val lastIndex = this.lastIndex
    if (lastIndex == 0) return minElem
    var minValue = selector(minElem)
    for (i in 1..lastIndex) {
        val e = this[i]
        val v = selector(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns the largest element or `null` if there are no elements.
 *
 * Since we code to Kotlin API 1.3, we copy this impl from the 1.4 stdlib
 */
fun <T : Comparable<T>> Iterable<T>.maxOrNull(): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var max = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (max < e) max = e
    }
    return max
}

/**
 * Removes the last element from this mutable list and returns that removed element, or throws [NoSuchElementException] if this list is empty.
 */
fun <T> MutableList<T>.removeLast(): T = if (isEmpty()) throw NoSuchElementException("List is empty.") else removeAt(lastIndex)

fun Appendable.appendLine(): Appendable = append('\n')
fun Appendable.appendLine(value: CharSequence?): Appendable = append(value).appendLine()

/**
 * Returns the largest value among all values produced by [selector] function
 * applied to each element in the array.
 *
 * @throws NoSuchElementException if the array is empty.
 */
inline fun <T, R : Comparable<R>> List<T>.maxOf(selector: (T) -> R): R {
    if (isEmpty()) throw NoSuchElementException()
    var maxValue = selector(this[0])
    for (i in 1..lastIndex) {
        val v = selector(this[i])
        if (maxValue < v) {
            maxValue = v
        }
    }
    return maxValue
}

inline fun <reified T : Annotation> KAnnotatedElement.hasAnnotation(): Boolean =
    findAnnotation<T>() != null
