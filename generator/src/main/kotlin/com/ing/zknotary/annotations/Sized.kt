package com.ing.zknotary.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Sized

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
/** Only applicable to collections, otherwise ignored.
 */
annotation class FixToLength(val size: Int = -1)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
annotation class UseDefault

class SizedList<T> private constructor(val list: List<T>, val originalSize: Int) {
    constructor(n: Int, default: T) : this(
        list = List(n) { default },
        originalSize = 0
    )
    constructor(n: Int, list: List<T>, default: T) : this (
        list = if (list.size <= n) {
            list + List(n - list.size) { default }
        } else {
            error("Actual size ${list.size} of the list exceeds expected size $n")
        },
        originalSize = list.size
    )
}

class SizedString private constructor(val string: String, val originalLength: Int) {
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
