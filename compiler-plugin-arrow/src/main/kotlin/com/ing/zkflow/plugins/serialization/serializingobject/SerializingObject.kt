package com.ing.zkflow.plugins.serialization.serializingobject

/**
 * This class represents a description of the KSerializers required to serialize a type
 * and provides an interface to convert itself into a sequence of pieces of Kotlin code (via SerializationSupport)
 * defining the appropriate chain of serializers.
 * That interface is accessible by a direct instance invocation with a parameter name which returns
 * an instance of SerializationSupport with the relevant serializers.
 * The instance of SerializingObject directly applicable for the given parameter will be accessible from SerializationSupport.
 */
abstract class SerializingObject {
    /**
     * Declaration of a type parameter for this object excluding all annotations.
     */
    internal abstract val cleanTypeDeclaration: String

    /**
     * Re-declaration of a parameter including new @Serializable(with = ...) annotation.
     */
    internal abstract val redeclaration: String

    internal abstract fun wrapDefault(): SerializingObject
    internal abstract fun wrapNull(): SerializingObject

    internal abstract operator fun invoke(outer: Tracker): SerializationSupport
    internal operator fun invoke(outer: String): SerializationSupport = invoke(Tracker(outer, listOf(Coordinate.Numeric())))
}
