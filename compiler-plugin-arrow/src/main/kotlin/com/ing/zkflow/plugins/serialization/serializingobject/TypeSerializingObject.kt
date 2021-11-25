package com.ing.zkflow.plugins.serialization.serializingobject

import com.ing.zkflow.Default
import com.ing.zkflow.Resolver
import com.ing.zkflow.plugins.serialization.annotationOrNull
import com.ing.zkflow.plugins.serialization.annotationSingleArgOrNull
import com.ing.zkflow.plugins.serialization.extractRootType
import com.ing.zkflow.serialization.serializer.KSerializerWithDefault
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import org.jetbrains.kotlin.psi.KtTypeReference
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * TypeSerializingObjects provide serializers for either explicitly known types, such as List, Int, Map, etc.
 * (full list is a key set of the `native` field in Processors)
 * and unknown types, such as 3rd party types and user classes.
 */
sealed class TypeSerializingObject : SerializingObject() {
    abstract val original: KtTypeReference

    override fun wrapDefault(): SerializingObject {
        // Inspect annotations to find either @com.ing.zkflow.Default or @com.ing.zkflow.Resolver
        val defaultProvider = with(original) {
            annotationSingleArgOrNull<Default<*>>()
                ?: annotationOrNull<Resolver<*, *>>()?.let {
                    it.valueArguments.getOrNull(0)?.asElement()?.text
                }
                ?: error("Element $text requires either a ${Default::class} or ${Resolver::class} annotation")
        }.replace("::class", "").trim()

        return ServiceSerializingObject.WithDefault(this, defaultProvider)
    }

    override fun wrapNull(): SerializingObject = ServiceSerializingObject.Nullable(wrapDefault())

    /**
     * Represents a serializing object for native types (full list is a key set of the `native` field in Processors).
     */
    class ExplicitType internal constructor(
        override val original: KtTypeReference,
        serializer: KClass<out KSerializer<*>>,
        private val type: String,
        private val children: List<SerializingObject>,
        private val construction: (self: ExplicitType, outer: Tracker, inner: List<Tracker>) -> String
    ) : TypeSerializingObject() {
        private val hasDefault = serializer.isSubclassOf(KSerializerWithDefault::class)

        override val cleanTypeDeclaration: String by lazy {
            children
                .joinToString(separator = ", ") { it.cleanTypeDeclaration }
                .let { if (it.isNotBlank()) "<$it>" else it }
                .let { inner -> "$type$inner" }
        }

        override val redeclaration: String by lazy {
            val annotationsDeclaration = original.annotationEntries.joinToString(separator = " ") { it.text }

            val inner = children
                .joinToString(separator = ", ") { it.redeclaration }
                .let { if (it.isNotBlank()) "<$it>" else it }

            "$annotationsDeclaration @${Contextual::class.qualifiedName!!} $type$inner"
        }

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
        override val original: KtTypeReference,
        private val construction: (self: UserType, outer: Tracker) -> String
    ) : TypeSerializingObject() {
        override val cleanTypeDeclaration: String by lazy {
            deriveCleanTypeDeclaration(original, ignoreNullability = true)
        }

        override val redeclaration: String by lazy {
            attachContextualAnnotation(original, ignoreNullability = true)
        }

        override fun invoke(outer: Tracker) = SerializationSupport(outer, construction(this, outer))

        /**
         * Recursively build the clean type tree.
         * E.g. for  `List<@Annotation CustomType<Int>>`:
         * 1. `List<@Annotation CustomType<Int>>` -> List
         * 2. `CustomType<Int>` -> CustomType
         * 3. `Int` -> Int
         * =. List<CustomType<Int>>
         */
        private fun deriveCleanTypeDeclaration(typeReference: KtTypeReference, ignoreNullability: Boolean): String {
            val type = typeReference.typeElement
            require(type != null) { "Cannot infer type of: `${typeReference.text}`" }

            val rootType = type.extractRootType()
            val nullability = if (ignoreNullability) "" else if (rootType.isNullable) "?" else ""

            return if (type.typeArgumentsAsTypes.isEmpty()) {
                "${rootType.type}$nullability"
            } else {
                "${rootType.type}${
                type.typeArgumentsAsTypes.joinToString(
                    prefix = "<",
                    separator = ", ",
                    postfix = ">"
                ) { deriveCleanTypeDeclaration(it, ignoreNullability = false) }
                }$nullability"
            }
        }

        /**
         * Recursively attach `Contextual` annotation.
         * E.g. for  `List<@Annotation CustomType<Int>>`:
         * 1. `List<@Annotation CustomType<Int>>` -> @Contextual List
         * 2. `CustomType<Int>` -> @Contextual CustomType
         * 3. `Int` -> @Contextual Int
         * =. @Contextual List<@Contextual CustomType<@Contextual Int>>
         */
        private fun attachContextualAnnotation(typeReference: KtTypeReference, ignoreNullability: Boolean): String {
            val type = typeReference.typeElement
            require(type != null) { "Cannot infer type of: `${typeReference.text}`" }

            val annotationsDeclaration = (
                typeReference.annotationEntries.map { it.text } +
                    "@${Contextual::class.qualifiedName!!}"
                ).joinToString(separator = " ")

            val rootType = type.extractRootType()
            val nullability = if (ignoreNullability) "" else if (rootType.isNullable) "?" else ""

            return if (type.typeArgumentsAsTypes.isEmpty()) {
                "$annotationsDeclaration ${rootType.type}$nullability"
            } else {
                "$annotationsDeclaration ${rootType.type}${
                type.typeArgumentsAsTypes.joinToString(
                    prefix = "<",
                    separator = ", ",
                    postfix = ">"
                ) { attachContextualAnnotation(it, ignoreNullability = false) }
                }$nullability"
            }
        }
    }
}
