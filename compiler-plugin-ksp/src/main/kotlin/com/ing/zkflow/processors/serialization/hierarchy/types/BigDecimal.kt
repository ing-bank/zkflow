package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.symbol.KSType
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.ksp.getNonRepeatableAnnotationByType
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName

internal fun KSType.asBigDecimal(tracker: Tracker): SerializingHierarchy {
    val (intLength, fracLength) = this.getNonRepeatableAnnotationByType(BigDecimalSize::class)
        .arguments
        .let { args -> Pair(args[0].value!!, args[1].value!!) }

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(FixedLengthFloatingPointSerializer.BigDecimalSerializer::class)
        .addSuperclassConstructorParameter("%L, %L", intLength, fracLength)
        .build()

    return SerializingHierarchy.OfType(this.toClassName(), emptyList(), serializingObject)
}
