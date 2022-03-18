package com.ing.zkflow.util

fun <T> List<T>.extendTo(newSize: Int, default: T): List<T> {
    require(size <= newSize) {
        "List size ($size) is larger than requested size ($newSize)."
    }
    return this + List(newSize - size) {
        default
    }
}

fun <T> List<T>.shrinkTo(newSize: Int): List<T> {
    require(newSize <= size) {
        "Requested size ($newSize) is larger than actual size ($size)."
    }
    return this.subList(0, newSize)
}
