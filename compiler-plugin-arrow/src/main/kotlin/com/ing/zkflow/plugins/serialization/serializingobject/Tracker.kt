package com.ing.zkflow.plugins.serialization.serializingobject

/**
 * Tracker is used to uniquely identify objects in the hierarchy of serializing objects.
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
