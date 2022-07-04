package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSTypeReference
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

internal fun KSTypeReference.asWithDigestAlgorithm(tracker: Tracker, strategy: KClass<out KSerializer<*>>, digestAlgorithm: ClassName): SerializingHierarchy {
    val type = resolve()

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(strategy.asClassName())
        .addSuperclassConstructorParameter(
            CodeBlock.of("%T::class", digestAlgorithm)
        )
        .build()

    return SerializingHierarchy.OfType(
        type.toClassName(),
        emptyList(),
        serializingObject
    )
}
