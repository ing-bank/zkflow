package com.ing.zkflow.plugins.serialization.serializingobject

import com.ing.zkflow.plugins.serialization.Tracker

/**
 * A utility class to roll up a hierarchy of serializing objects into a sequence of pieces of Kotlin code defining corresponding objects.
 * E.g., for parameter `param: @Size(5) List<Int>`, its serialization support is:
 * entry: Param_0
 * objects:
 *      `Param_0: FixedLengthList(5, Param_1)`
 *      `Param_1: WrappedKSerializer(IntSerializer)`
 */
data class SerializationSupport(private val entry: Tracker, val objects: List<String>) {
    constructor(entry: Tracker, value: String) : this(entry, listOf(value))

    val serializer = "$entry::class"

    fun include(subsumed: SerializationSupport) = SerializationSupport(entry, objects + subsumed.objects)
}
