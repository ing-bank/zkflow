package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSType
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

internal fun KSType.asInt(tracker: Tracker): SerializingHierarchy {
    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            WrappedFixedLengthKSerializerWithDefault::class
                .asClassName()
                .parameterizedBy(this.toClassName())
        )
        .addSuperclassConstructorParameter("%T", IntSerializer::class)
        .build()

    return SerializingHierarchy.OfType(this.toClassName(), emptyList(), serializingObject)
}
