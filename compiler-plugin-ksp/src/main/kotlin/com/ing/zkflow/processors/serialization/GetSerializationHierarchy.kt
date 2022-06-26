package com.ing.zkflow.processors.serialization

import com.google.devtools.ksp.symbol.KSType
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.ksp.getSingleArgumentOfNonRepeatableAnnotationByType
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

@Suppress("LongMethod")
internal fun KSType.getSerializingHierarchy(tracker: Tracker, ignoreNullability: Boolean = false, mustHaveDefault: Boolean = false): SerializingHierarchy {
    if (this.isMarkedNullable && !ignoreNullability) {
        // this.makeNotNullable effectively removes annotations, that is, one can see them in the debugger, but they are inaccessible.
        val inner = this.getSerializingHierarchy(tracker.next(), ignoreNullability = true, mustHaveDefault = true)
        require(inner is SerializingHierarchy.OfType) { "Type $declaration cannot be marked as nullable twice" }

        val typeSpec = TypeSpec.objectBuilder("$tracker")
            .addModifiers(KModifier.PRIVATE)
            .superclass(
                NullableSerializer::class
                    .asClassName()
                    .parameterizedBy(inner.type)
            )
            .addSuperclassConstructorParameter(CodeBlock.of("%N", inner.serializingObject))
            .build()

        return SerializingHierarchy.OfNullable(inner, typeSpec)
    }

    // Invariant:
    // Nullability has been stripped by now.

    // TODO this treatment must be done only for types whuch serializers have no default.
    //  Because we know all the serializers, we can look for defaults only in a very specific cases
    // if (mustHaveDefault) {
    //     val defaultProvider = (this as KSAnnotated).getSingleArgumentOfSingleAnnotationByType(Default::class)
    //         .toString()
    //         .dropLast("::class".length)
    //
    //     val inner = this.getSerializingObject(tracker.next(), mustHaveDefault = false)
    //
    //     val typeSpec = TypeSpec.objectBuilder("$tracker")
    //         .addModifiers(KModifier.PRIVATE)
    //         .superclass(
    //             SerializerWithDefault::class
    //                 .asClassName()
    //                 .parameterizedBy(inner.type)
    //         )
    //         .addSuperclassConstructorParameter(CodeBlock.of("%N, %L.default", inner.serializingObject, defaultProvider))
    //         .build()
    //
    //     return SerializingHierarchy.OfType(this.toClassName(), listOf(inner), typeSpec)
    // }

    val fqName = this.declaration.qualifiedName?.asString() ?: error("Cannot determine a fully qualified name of $declaration")

    return when (fqName) {
        Int::class.qualifiedName -> {
            val typeSpec = TypeSpec.objectBuilder("$tracker")
                .addModifiers(KModifier.PRIVATE)
                .superclass(
                    WrappedFixedLengthKSerializerWithDefault::class
                        .asClassName()
                        .parameterizedBy(this.toClassName())
                )
                .addSuperclassConstructorParameter("%T", IntSerializer::class)
                .build()

            SerializingHierarchy.OfType(this.toClassName(), listOf(), typeSpec)
        }

        List::class.qualifiedName -> {
            val maxSize = this.getSingleArgumentOfNonRepeatableAnnotationByType(Size::class)

            val innerType = this.arguments[0].type?.resolve() ?: error("Cannot resolve a type argument of $declaration")
            val innerSerializingObject = innerType.getSerializingHierarchy(tracker.next(), mustHaveDefault = true)

            val typeSpec = TypeSpec.objectBuilder("$tracker")
                .addModifiers(KModifier.PRIVATE)
                .superclass(
                    FixedLengthListSerializer::class
                        .asClassName()
                        .parameterizedBy(innerSerializingObject.type)
                )
                .addSuperclassConstructorParameter(
                    CodeBlock.of("%L, %N", maxSize, innerSerializingObject.serializingObject)
                )
                .build()

            SerializingHierarchy.OfType(
                this.toClassName(),
                listOf(innerSerializingObject),
                typeSpec
            )
        }

        Map::class.qualifiedName -> {
            val maxSize = this.getSingleArgumentOfNonRepeatableAnnotationByType(Size::class)

            val keyType = this.arguments[0].type?.resolve() ?: error("Cannot resolve a type argument of $declaration")
            val keySerializingObject = keyType.getSerializingHierarchy(tracker.literal(0).numeric(), mustHaveDefault = true)

            val valueType = this.arguments[1].type?.resolve() ?: error("Cannot resolve a type argument of $declaration")
            val valueSerializingObject = valueType.getSerializingHierarchy(tracker.literal(1).numeric(), mustHaveDefault = true)

            val typeSpec = TypeSpec.objectBuilder("$tracker")
                .addModifiers(KModifier.PRIVATE)
                .superclass(
                    FixedLengthMapSerializer::class
                        .asClassName()
                        .parameterizedBy(keySerializingObject.type, valueSerializingObject.type)
                    // TODO: Possible to copy annotations, but difficult and unclear why we would need that.
                    // .copy(annotations = annotations.map { .. })
                )
                .addSuperclassConstructorParameter(
                    CodeBlock.of("%L, %N, %N", maxSize, keySerializingObject.serializingObject, valueSerializingObject.serializingObject)
                )
                .build()

            SerializingHierarchy.OfType(
                this.toClassName(),
                listOf(keySerializingObject, valueSerializingObject),
                typeSpec
            )
        }

        else -> TODO("Unsupported type:  $fqName; ${if (mustHaveDefault) "Default value is required" else "Default value is NOT required"}")
    }
}
