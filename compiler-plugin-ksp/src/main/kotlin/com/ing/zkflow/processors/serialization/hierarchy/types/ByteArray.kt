package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSTypeReference
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.ksp.getSingleArgumentOfNonRepeatableAnnotationByType
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

internal fun KSTypeReference.asByteArray(tracker: Tracker): SerializingHierarchy {
    val type = resolve()

    val maxSize = getSingleArgumentOfNonRepeatableAnnotationByType(Size::class)

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(FixedLengthByteArraySerializer::class.asClassName())
        .addSuperclassConstructorParameter(CodeBlock.of("%L", maxSize))
        .build()

    return SerializingHierarchy.OfType(
        type.toClassName(),
        emptyList(),
        serializingObject
    )
}
