package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSTypeReference
import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.UTF16
import com.ing.zkflow.annotations.UTF32
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.ksp.getSingleArgumentOfNonRepeatableAnnotationByType
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.serialization.serializer.string.FixedSizeAsciiStringSerializer
import com.ing.zkflow.serialization.serializer.string.FixedSizeUtf16StringSerializer
import com.ing.zkflow.serialization.serializer.string.FixedSizeUtf32StringSerializer
import com.ing.zkflow.serialization.serializer.string.FixedSizeUtf8StringSerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName

internal fun KSTypeReference.asString(tracker: Tracker): SerializingHierarchy {
    val type = resolve()

    val serializingObjectBuilder = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)

    val possibleCases = mapOf(
        ASCII::class to FixedSizeAsciiStringSerializer::class,
        UTF8::class to FixedSizeUtf8StringSerializer::class,
        UTF16::class to FixedSizeUtf16StringSerializer::class,
        UTF32::class to FixedSizeUtf32StringSerializer::class,
    )

    // It is ensured elsewhere that one of these annotations is present.
    for ((annotation, serializer) in possibleCases) {
        val size = try {
            this.getSingleArgumentOfNonRepeatableAnnotationByType(annotation)
        } catch (_: Exception) {
            continue
        }

        serializingObjectBuilder
            .superclass(serializer)
            .addSuperclassConstructorParameter("%L", size)

        break
    }

    return SerializingHierarchy.OfType(type.toClassName(), emptyList(), serializingObjectBuilder.build())
}
