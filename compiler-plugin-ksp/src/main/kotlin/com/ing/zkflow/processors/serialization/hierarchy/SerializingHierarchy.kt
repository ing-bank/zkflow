package com.ing.zkflow.processors.serialization.hierarchy

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.ing.zkflow.processors.serialization.hierarchy.types.asBasic
import com.ing.zkflow.processors.serialization.hierarchy.types.asBigDecimal
import com.ing.zkflow.processors.serialization.hierarchy.types.asChar
import com.ing.zkflow.processors.serialization.hierarchy.types.asList
import com.ing.zkflow.processors.serialization.hierarchy.types.asMap
import com.ing.zkflow.processors.serialization.hierarchy.types.asNullable
import com.ing.zkflow.processors.serialization.hierarchy.types.asSet
import com.ing.zkflow.processors.serialization.hierarchy.types.asString
import com.ing.zkflow.processors.serialization.hierarchy.types.asUserType
import com.ing.zkflow.serialization.serializer.BooleanSerializer
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.LongSerializer
import com.ing.zkflow.serialization.serializer.ShortSerializer
import com.ing.zkflow.serialization.serializer.UByteSerializer
import com.ing.zkflow.serialization.serializer.UIntSerializer
import com.ing.zkflow.serialization.serializer.ULongSerializer
import com.ing.zkflow.serialization.serializer.UShortSerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.Contextual
import java.math.BigDecimal

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
        // Basic types.
        Boolean::class.qualifiedName -> asBasic(tracker, BooleanSerializer::class)
        Byte::class.qualifiedName -> asBasic(tracker, ByteSerializer::class)
        UByte::class.qualifiedName -> asBasic(tracker, UByteSerializer::class)
        Short::class.qualifiedName -> asBasic(tracker, ShortSerializer::class)
        UShort::class.qualifiedName -> asBasic(tracker, UShortSerializer::class)
        Int::class.qualifiedName -> asBasic(tracker, IntSerializer::class)
        UInt::class.qualifiedName -> asBasic(tracker, UIntSerializer::class)
        Long::class.qualifiedName -> asBasic(tracker, LongSerializer::class)
        ULong::class.qualifiedName -> asBasic(tracker, ULongSerializer::class)
        Float::class.qualifiedName -> asBasic(tracker, FixedLengthFloatingPointSerializer.FloatSerializer::class)
        Double::class.qualifiedName -> asBasic(tracker, FixedLengthFloatingPointSerializer.DoubleSerializer::class)
        Char::class.qualifiedName -> asChar(tracker)
        String::class.qualifiedName -> asString(tracker)
        BigDecimal::class.qualifiedName -> asBigDecimal(tracker)
        //
        // Generic collections.
        List::class.qualifiedName -> asList(tracker)
        Map::class.qualifiedName -> asMap(tracker)
        Set::class.qualifiedName -> asSet(tracker)

        //
        else -> asUserType(tracker, mustHaveDefault)
    }
}
