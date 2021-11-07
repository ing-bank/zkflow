package com.ing.zkflow.plugins.serialization

/**
 * Tracker to keep track of serializing objects hierarchies.
 *
 * This class serves the following purpose:
 * For a type, a tracker, say, `Name_0` is initialized,
 * if there is a single inner type of this type, the inner type receives a tracker `Name_1`,
 * if there are multiple inner types, the inner types receive trackers `Name_0_A_0` and `Name_0_B_0`
 */
data class Tracker(val name: String, val coordinates: List<Coordinate>) {
    override fun toString() = "${name}_${coordinates.joinToString(separator = "_")}"
    fun next() = Tracker(name, coordinates.subList(0, coordinates.size - 1) + coordinates.last().next())

    fun literal(nth: Int = 0) = Tracker(name, coordinates + Coordinate.Literal().next(nth))
    fun numeric(nth: Int = 0) = Tracker(name, coordinates + Coordinate.Numeric().next(nth))
}

sealed class Coordinate {
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
            require(times >= 1) { "Argument must be >= 1" }
            return if (times == 1) next() else next().next(times - 1)
        }

        override fun toString() = s.joinToString(separator = "")
    }

    abstract fun next(): Coordinate
    abstract fun next(times: Int): Coordinate
}
