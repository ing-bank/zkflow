package com.ing.zkflow.tracking

/**
 * A utility class allowing two kinds of linear "coordinates"
 * and sequential progression for each type.
 *
 * A Numeric coordinate is a natural number, i.e., 1, 2, 3,...
 * A Literal coordinate is an n-letter composition of the English alphabet letters, i.e., A, B, ... C, AA, AB, ... ZZ, AAA, ...
 */
sealed class Coordinate {
    abstract fun next(): Coordinate
    abstract fun next(times: Int): Coordinate

    data class Numeric(private val i: Int = 0) : Coordinate() {
        override fun next() = Numeric(i + 1)
        override fun next(times: Int) = Numeric(i + times)
        override fun toString() = "$i"
    }

    data class Literal(private val s: List<Char> = listOf('A')) : Coordinate() {
        /**
         * Progresses in steps {A}, {B}, {C},... {Z}, {A, A}, {A, B},..., {Z, Z}, {A, A, A}, ...
         * */
        override fun next(): Coordinate =
            s.foldRight(Pair(true, mutableListOf<Char>())) { item, (carry, acc) ->
                if (carry) {
                    if (item == 'Z') {
                        acc.add('A')
                        Pair(true, acc)
                    } else {
                        acc.add(item + 1)
                        Pair(false, acc)
                    }
                } else {
                    acc.add(item)
                    Pair(false, acc)
                }
            }.let { (carry, nextValue) ->
                if (carry) {
                    Literal(List(s.size + 1) { 'A' })
                } else {
                    Literal(nextValue.reversed())
                }
            }

        override fun next(times: Int): Coordinate {
            require(times >= 0) { "Argument for Tracker must be non-negative" }
            return if (times == 0) this else next().next(times - 1)
        }

        override fun toString() = s.joinToString(separator = "")
    }
}
