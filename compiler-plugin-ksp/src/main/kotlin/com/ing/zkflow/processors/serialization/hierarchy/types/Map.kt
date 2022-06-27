package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSType
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

internal fun KSType.asMap(tracker: Tracker): SerializingHierarchy {
    val maxSize = this.getSingleArgumentOfNonRepeatableAnnotationByType(Size::class)

    val keyType = this.arguments[0].type?.resolve() ?: error("Cannot resolve a type argument of $declaration")
    val keySerializingObject = keyType.getSerializingHierarchy(tracker.literal(0).numeric(), mustHaveDefault = true)

    val valueType = this.arguments[1].type?.resolve() ?: error("Cannot resolve a type argument of $declaration")
    val valueSerializingObject = valueType.getSerializingHierarchy(tracker.literal(1).numeric(), mustHaveDefault = true)

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            FixedLengthMapSerializer::class
                .asClassName()
                .parameterizedBy(keySerializingObject.type, valueSerializingObject.type)
            // TODO: Possible to copy annotations, but difficult and unclear why we would need that.
            // .copy(annotations = annotations.map { .. })
        )
        .addSuperclassConstructorParameter(
            CodeBlock.of("%L, %N, %N", maxSize, keySerializingObject.definition, valueSerializingObject.definition)
        )
        .build()

    return SerializingHierarchy.OfType(
        this.toClassName(),
        listOf(keySerializingObject, valueSerializingObject),
        serializingObject
    )
}
