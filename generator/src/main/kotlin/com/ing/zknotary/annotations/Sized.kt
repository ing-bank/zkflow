package com.ing.zknotary.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Sized(val size: Int = -1)

data class WrappedList<T> private constructor(val list: List<T>, val originalSize: Int) {
    constructor(n: Int, default: T) : this(
        list = List(n) { default },
        originalSize = 0
    )
    constructor(n: Int, list: List<T>, default: T) :  this (
        list = if (list.size <= n )  {
            List(n) {
                if (it < list.size) {
                    list[it]
                } else {
                    default
                }
            }
        } else {
            error("Actual size ${list.size} of the list exceeds expected size $n")
        },
        originalSize = list.size
    )
}
