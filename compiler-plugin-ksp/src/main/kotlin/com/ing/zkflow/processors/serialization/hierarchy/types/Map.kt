package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSTypeReference
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.ksp.getSingleArgumentOfNonRepeatableAnnotationByType
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.processors.serialization.hierarchy.getSerializingHierarchy
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

internal fun KSTypeReference.asMap(tracker: Tracker): SerializingHierarchy {
    val type = resolve()

    val maxSize = getSingleArgumentOfNonRepeatableAnnotationByType(Size::class)

    val keyType = type.arguments[0].type ?: error("Cannot resolve a type argument of ${type.declaration}")
    val keySerializingHierarchy = keyType.getSerializingHierarchy(tracker.literal(0).numeric(), mustHaveDefault = true)

    val valueType = type.arguments[1].type ?: error("Cannot resolve a type argument of ${type.declaration}")
    val valueSerializingHierarchy = valueType.getSerializingHierarchy(tracker.literal(1).numeric(), mustHaveDefault = true)

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            FixedLengthMapSerializer::class
                .asClassName()
                .parameterizedBy(keySerializingHierarchy.type, valueSerializingHierarchy.type)
            // TODO: Possible to copy annotations, but difficult and unclear why we would need that.
            // .copy(annotations = annotations.map { .. })
        )
        .addSuperclassConstructorParameter(
            CodeBlock.of("%L, %N, %N", maxSize, keySerializingHierarchy.definition, valueSerializingHierarchy.definition)
        )
        .build()

    return SerializingHierarchy.OfType(
        type.toClassName(),
        listOf(keySerializingHierarchy, valueSerializingHierarchy),
        serializingObject
    )
}
