package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSType
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.ksp.getSingleArgumentOfNonRepeatableAnnotationByType
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.processors.serialization.hierarchy.getSerializingHierarchy
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

internal fun KSType.asList(tracker: Tracker): SerializingHierarchy {
    val maxSize = this.getSingleArgumentOfNonRepeatableAnnotationByType(Size::class)

    val innerType = this.arguments[0].type?.resolve() ?: error("Cannot resolve a type argument of $declaration")
    val innerSerializingHierarchy = innerType.getSerializingHierarchy(tracker.next(), mustHaveDefault = true)

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            FixedLengthListSerializer::class
                .asClassName()
                .parameterizedBy(innerSerializingHierarchy.type)
        )
        .addSuperclassConstructorParameter(
            CodeBlock.of("%L, %N", maxSize, innerSerializingHierarchy.definition)
        )
        .build()

    return SerializingHierarchy.OfType(
        this.toClassName(),
        listOf(innerSerializingHierarchy),
        serializingObject
    )
}
