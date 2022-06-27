package com.ing.zkflow.processors.serialization.hierarchy

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.ing.zkflow.processors.serialization.hierarchy.types.asInt
import com.ing.zkflow.processors.serialization.hierarchy.types.asList
import com.ing.zkflow.processors.serialization.hierarchy.types.asMap
import com.ing.zkflow.processors.serialization.hierarchy.types.asNullable
import com.ing.zkflow.processors.serialization.hierarchy.types.asUserType
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.Contextual

internal fun KSValueParameter.getSerializingHierarchy(): SerializingHierarchy {
    val name = name?.asString() ?: error("Cannot get a name of $this")
    this.type.resolve().exhibitFunnyBehavior()
    return this.type.resolve().getSerializingHierarchy(Tracker(name.capitalize()))
}

private fun KSType.exhibitFunnyBehavior() {
    val a = this.annotations.toList()
    val thisNotNullable = this.makeNotNullable()
    val b = thisNotNullable.annotations.toList()
    if (a != b) {
        println("Is this expected??? I did not expect this")
    }
}

internal sealed class SerializingHierarchy(
    val definition: TypeSpec
) {
    class OfType(
        private val rootType: ClassName,
        val inner: List<SerializingHierarchy>,
        serializingObject: TypeSpec,
    ) : SerializingHierarchy(serializingObject) {
        override val declaration: TypeName
            get() {
                val annotations = listOf(AnnotationSpec.builder(Contextual::class).build())
                return if (inner.isEmpty()) {
                    rootType.copy(annotations = annotations)
                } else {
                    rootType
                        .parameterizedBy(inner.map { it.declaration })
                        .copy(annotations = annotations)
                }
            }

        override val type: TypeName
            get() = if (inner.isEmpty()) {
                rootType.copy(annotations = emptyList())
            } else {
                rootType
                    .parameterizedBy(inner.map { it.type })
                    .copy(annotations = emptyList())
            }
    }

    class OfNullable(
        val inner: SerializingHierarchy,
        serializingObject: TypeSpec
    ) : SerializingHierarchy(serializingObject) {
        override val declaration: TypeName
            get() = inner.declaration.copy(nullable = true)
        override val type: TypeName
            get() = inner.type.copy(nullable = true)
    }

    class Placeholder(
        val inner: SerializingHierarchy,
        serializingObject: TypeSpec
    ) : SerializingHierarchy(serializingObject) {
        override val declaration: TypeName
            get() = inner.declaration
        override val type: TypeName
            get() = inner.type
    }

    abstract val declaration: TypeName
    abstract val type: TypeName
    fun addTypesTo(container: TypeSpec.Builder) {
        container.addType(definition)
        when (this) {
            is OfType -> inner.forEach { it.addTypesTo(container) }
            is OfNullable -> inner.addTypesTo(container)
            is Placeholder -> inner.addTypesTo(container)
        }
    }
}

@Suppress("LongMethod", "ComplexMethod")
internal fun KSType.getSerializingHierarchy(tracker: Tracker, ignoreNullability: Boolean = false, mustHaveDefault: Boolean = false): SerializingHierarchy {
    if (this.isMarkedNullable && !ignoreNullability) {
        return this.asNullable(tracker)
    }

    // Invariant:
    // Nullability has been stripped by now.

    val fqName = this.declaration.qualifiedName?.asString() ?: error("Cannot determine a fully qualified name of $declaration")

    return when (fqName) {
        Int::class.qualifiedName -> this.asInt(tracker)
        List::class.qualifiedName -> this.asList(tracker)
        Map::class.qualifiedName -> this.asMap(tracker)
        //
        else -> this.asUserType(tracker, mustHaveDefault)
    }
}
