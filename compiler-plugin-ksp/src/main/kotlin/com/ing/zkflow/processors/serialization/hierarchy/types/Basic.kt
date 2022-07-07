package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSTypeReference
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

internal fun KSTypeReference.asBasic(tracker: Tracker, strategy: KClass<out KSerializer<*>>): SerializingHierarchy {
    val type = resolve()

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            WrappedFixedLengthKSerializerWithDefault::class
                .asClassName()
                .parameterizedBy(type.toClassName())
        )
        .addSuperclassConstructorParameter("%T", strategy)
        .build()

    return SerializingHierarchy.OfType(type.toClassName(), emptyList(), serializingObject)
}
