package com.ing.zkflow.plugins.serialization.serializingobject

/**
 * A utility class to roll up a hierarchy of serializing objects into a sequence of pieces of Kotlin code defining corresponding objects.
 * `entry` is a Tracker instance referring to the first serializing object in the hierarchy.
 *
 * E.g., for parameter `param: @Size(5) List<Int>`, its serialization support is:
 * entry: Param_0
 * serializingObjectKotlinDefinitions:
 *      `Param_0: FixedLengthList(5, Param_1)`
 *      `Param_1: WrappedFixedLengthKSerializer(IntSerializer)`
 */
data class SerializationSupport(private val entry: Tracker, val serializingObjectKotlinDefinitions: List<String>) {
    constructor(entry: Tracker, value: String) : this(entry, listOf(value))

    val serializer = "$entry::class"

    fun include(included: SerializationSupport) = SerializationSupport(entry, serializingObjectKotlinDefinitions + included.serializingObjectKotlinDefinitions)
}
