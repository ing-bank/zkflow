package com.ing.zkflow.plugins.serialization.serializingobject

import com.ing.zkflow.plugins.serialization.Tracker
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import com.ing.zkflow.serialization.serializer.NullableSerializer

/**
 * Service serializing objects provide wrappings for nullable types and types that require a default value.
 */
sealed class ServiceSerializingObject : SerializingObject() {
    abstract val child: SerializingObject
    abstract val construction: (outer: Tracker, inner: Tracker) -> String

    override fun wrapDefault(): SerializingObject = this
    override fun wrapNull(): SerializingObject = Nullable(this)

    override fun invoke(outer: Tracker): SerializationSupport {
        val inner = outer.next()
        return SerializationSupport(outer, construction(outer, inner)).include(child(inner))
    }

    /**
     * Wraps a child serializing object into NullableSerializer.
     */
    class Nullable internal constructor(
        override val child: SerializingObject,
    ) : ServiceSerializingObject() {
        override val cleanTypeDeclaration = "${child.cleanTypeDeclaration}?"
        override val redeclaration = "${child.redeclaration}?"
        override val construction = { outer: Tracker, inner: Tracker ->
            "object $outer: ${NullableSerializer::class.qualifiedName}<${child.cleanTypeDeclaration}>($inner)"
        }
    }

    /**
     * Attaches to a child serializing object a default value by constructing a KSerializerWithDefault object.
     */
    class WithDefault internal constructor(
        override val child: SerializingObject,
        defaultProvider: String
    ) : ServiceSerializingObject() {
        override val cleanTypeDeclaration = child.cleanTypeDeclaration
        override val redeclaration = child.redeclaration
        override val construction = { outer: Tracker, inner: Tracker ->
            "object $outer: ${KSerializerWithDefault::class.qualifiedName}<$cleanTypeDeclaration>($inner, $defaultProvider.default)"
        }
    }
}
