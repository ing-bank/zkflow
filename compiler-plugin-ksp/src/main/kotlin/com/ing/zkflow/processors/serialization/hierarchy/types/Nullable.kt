package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSType
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.processors.serialization.hierarchy.getSerializingHierarchy
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

internal fun KSType.asNullable(tracker: Tracker): SerializingHierarchy {
    // this.makeNotNullable effectively removes annotations, that is, one can see them in the debugger, but they are inaccessible.
    val inner = this.getSerializingHierarchy(tracker.next(), ignoreNullability = true, mustHaveDefault = true)
    require(inner !is SerializingHierarchy.OfNullable) { "Type $declaration cannot be marked as nullable twice" }

    val typeSpec = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            NullableSerializer::class
                .asClassName()
                .parameterizedBy(inner.type)
        )
        .addSuperclassConstructorParameter(CodeBlock.of("%N", inner.definition))
        .build()

    return SerializingHierarchy.OfNullable(inner, typeSpec)
}
