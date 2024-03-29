package com.ing.zkflow.zinc.types

public inline fun <T> generateDifferentValueThan(initialValue: T, generator: () -> T): T {
    var it = generator()
    while (it == initialValue) {
        it = generator()
    }
    return it
}
