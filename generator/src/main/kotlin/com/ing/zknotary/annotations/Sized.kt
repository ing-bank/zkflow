package com.ing.zknotary.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Sized

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
/**
 *  Only applicable to collections, otherwise ignored.
 */
annotation class FixToLength(val size: Int = -1)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
annotation class UseDefault

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
/**
 * Instructs the annotation processor to use `code` for constructing default elements.
 * `code` must be a valid kotlin code using only classes listed in `imports`.
 */
annotation class Call(val imports: Array<String>, val code: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
annotation class CallDefaultValueClass(val imports: Array<String>, val defaultValueClass: String)

interface DefaultValue {
    val default: Any
}

/**
 * Wraps Lists into lists of fixed size.
 * ALSO, wraps both generic and specialized arrays (ByteArray, CharArray, etc.)
 * The latter has a performance penalty, thus specific wrapper classes can be developed if the penalty is too high.
 */
class SizedList<T> private constructor(val list: List<T>, val originalSize: Int) : List<T> by list {
    companion object {
        fun <T> fromIterator(iterator: Iterator<T>, n: Int, default: T): SizedList<T> {
            val list = iterator.asSequence().toList()
            return SizedList(
                list = if (list.size <= n) {
                    list + List(n - list.size) { default }
                } else {
                    error("Actual size ${list.size} of the list exceeds expected size $n")
                },
                originalSize = list.size
            )
        }
    }

    constructor(n: Int, default: T) : this(
        list = List(n) { default },
        originalSize = 0
    )
}

class SizedString private constructor(val string: String, val originalLength: Int) : CharSequence by string {
    constructor(n: Int, default: Char) : this(
        string = List(n) { default }.joinToString(separator = ""),
        originalLength = 0
    )
    constructor(n: Int, string: String, default: Char) : this (
        string = if (string.length <= n) {
            string + List(n - string.length) { default }.joinToString(separator = "")
        } else {
            error("Actual length ${string.length} of the string exceeds expected size $n")
        },
        originalLength = string.length
    )
}
