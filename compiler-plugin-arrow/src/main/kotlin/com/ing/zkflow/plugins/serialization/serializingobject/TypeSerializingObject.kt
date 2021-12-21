package com.ing.zkflow.plugins.serialization.serializingobject

import com.ing.zkflow.Default
import com.ing.zkflow.Resolver
import com.ing.zkflow.plugins.serialization.ContextualizedKtTypeReference
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * TypeSerializingObjects provide serializers for either explicitly known types, such as List, Int, Map, etc.
 * (full list is a key set of the `native` field in Processors)
 * and unknown types, such as 3rd party types and user classes.
 */
sealed class TypeSerializingObject(private val contextualizedOriginal: ContextualizedKtTypeReference) : SerializingObject() {
    override fun wrapDefault(): SerializingObject {
        // Inspect annotations to find either @com.ing.zkflow.Default or @com.ing.zkflow.Resolver
        val defaultProvider = with(contextualizedOriginal) {
            annotationSingleArgOrNull<Default<*>>()
                ?: annotationOrNull<Resolver<*, *>>()?.let {
                    it.valueArguments.getOrNull(0)?.asElement()?.text
                }
                ?: error("Element ${ktTypeReference.text} requires either a ${Default::class} or ${Resolver::class} annotation")
        }.replace("::class", "").trim()

        return ServiceSerializingObject.WithDefault(this, defaultProvider)
    }

    override fun wrapNull(): SerializingObject = ServiceSerializingObject.Nullable(wrapDefault())

    override val cleanTypeDeclaration: String = contextualizedOriginal.stripAnnotations(ignoreNullability = true)
    override val redeclaration: String = contextualizedOriginal.attachAnnotation(Contextual::class, ignoreNullability = true)

    /**
     * Represents a serializing object for native types (full list is a key set of the `native` field in Processors).
     */
    class ExplicitType internal constructor(
        contextualizedOriginal: ContextualizedKtTypeReference,
        serializer: KClass<out KSerializer<*>>,
        private val children: List<SerializingObject>,
        private val construction: (self: ExplicitType, outer: Tracker, inner: List<Tracker>) -> String
    ) : TypeSerializingObject(contextualizedOriginal) {
        private val hasDefault = serializer.isSubclassOf(KSerializerWithDefault::class)

        override fun invoke(outer: Tracker): SerializationSupport {
            val inners = when (children.size) {
                0 -> return SerializationSupport(outer, construction(this, outer, emptyList()))
                1 -> listOf(outer.next())
                else -> List(children.size) { idx -> outer.literal(idx).numeric() }
            }

            return children.indices.fold(
                SerializationSupport(
                    outer,
                    construction(this, outer, inners)
                )
            ) { support, idx ->
                val inner = inners[idx]
                val child = children[idx]
                support.include(child(inner))
            }
        }

        override fun wrapDefault(): SerializingObject {
            return if (hasDefault) this else super.wrapDefault()
        }
    }

    /**
     * Represents a serializing object for user classes and 3rd party classes.
     */
    class UserType internal constructor(
        contextualizedOriginal: ContextualizedKtTypeReference,
        private val construction: (self: UserType, outer: Tracker) -> String
    ) : TypeSerializingObject(contextualizedOriginal) {
        override fun invoke(outer: Tracker) = SerializationSupport(outer, construction(this, outer))
    }
}
