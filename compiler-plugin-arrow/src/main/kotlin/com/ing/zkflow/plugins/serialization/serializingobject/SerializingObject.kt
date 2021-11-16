package com.ing.zkflow.plugins.serialization.serializingobject

import com.ing.zkflow.plugins.serialization.Coordinate
import com.ing.zkflow.plugins.serialization.Tracker

/**
 * Every serializable parameter requires an appropriate serializing object.
 * This class defines common functionality for all kinds of serializing objects
 * and provides an interface to convert such object into a sequence of pieces of Kotlin code (via SerializationSupport)
 * defining the appropriate chain of serializers.
 */
abstract class SerializingObject {
    internal abstract val cleanTypeDeclaration: String
    internal abstract val redeclaration: String

    internal abstract fun wrapDefault(): SerializingObject
    internal abstract fun wrapNull(): SerializingObject

    abstract operator fun invoke(outer: Tracker): SerializationSupport
    internal operator fun invoke(outer: String): SerializationSupport = invoke(Tracker(outer, listOf(Coordinate.Numeric())))
}
