package com.ing.zkflow.processors.serialization

import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zkflow.Default
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.ksp.getNonRepeatableAnnotationByType
import com.ing.zkflow.ksp.getSingleArgumentOfNonRepeatableAnnotationByType
import com.ing.zkflow.ksp.getSurrogateFromViaAnnotation
import com.ing.zkflow.ksp.getSurrogateSerializerClassName
import com.ing.zkflow.ksp.getSurrogateTargetClass
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.NullableSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

@Suppress("LongMethod", "ComplexMethod")
internal fun KSType.getSerializingHierarchy(tracker: Tracker, ignoreNullability: Boolean = false, mustHaveDefault: Boolean = false): SerializingHierarchy {
    if (this.isMarkedNullable && !ignoreNullability) {
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

    // Invariant:
    // Nullability has been stripped by now.

    val fqName = this.declaration.qualifiedName?.asString() ?: error("Cannot determine a fully qualified name of $declaration")

    return when (fqName) {
        Int::class.qualifiedName -> {
            val serializingObject = TypeSpec.objectBuilder("$tracker")
                .addModifiers(KModifier.PRIVATE)
                .superclass(
                    WrappedFixedLengthKSerializerWithDefault::class
                        .asClassName()
                        .parameterizedBy(this.toClassName())
                )
                .addSuperclassConstructorParameter("%T", IntSerializer::class)
                .build()

            SerializingHierarchy.OfType(this.toClassName(), emptyList(), serializingObject)
        }

        List::class.qualifiedName -> {
            val maxSize = this.getSingleArgumentOfNonRepeatableAnnotationByType(Size::class)

            val innerType = this.arguments[0].type?.resolve() ?: error("Cannot resolve a type argument of $declaration")
            val innerSerializingObject = innerType.getSerializingHierarchy(tracker.next(), mustHaveDefault = true)

            val serializingObject = TypeSpec.objectBuilder("$tracker")
                .addModifiers(KModifier.PRIVATE)
                .superclass(
                    FixedLengthListSerializer::class
                        .asClassName()
                        .parameterizedBy(innerSerializingObject.type)
                )
                .addSuperclassConstructorParameter(
                    CodeBlock.of("%L, %N", maxSize, innerSerializingObject.definition)
                )
                .build()

            SerializingHierarchy.OfType(
                this.toClassName(),
                listOf(innerSerializingObject),
                serializingObject
            )
        }

        Map::class.qualifiedName -> {
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

            SerializingHierarchy.OfType(
                this.toClassName(),
                listOf(keySerializingObject, valueSerializingObject),
                serializingObject
            )
        }

        else -> {
            // User type.
            // User type is serializable if
            // - this type is owned
            // - this type is a third-party class with a specified surrogate
            //
            // - Possibly must have a @Default annotation.

            // If `this` must have default, then extra level of indirection is required, therefore
            // surrogate serializer must take the next tracker, i.e.
            // (** Previous recursion level **)    (***********************   Current recursion level    ************************)
            //      NullableSerializers         ->  WrappedKSerializerWithDefault(tracker) -> SurrogateSerializer(tracker.next())
            //                                      ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
            val nextTracker = if (mustHaveDefault) tracker.next() else tracker

            val serializingObject = this.trySerializeAsOwnClass(nextTracker) ?: this.trySerializeAs3rdPartyClass(nextTracker) ?: error("")

            if (mustHaveDefault) {
                val defaultProvider = this.getSingleArgumentOfNonRepeatableAnnotationByType(Default::class) as KSType

                val serializingObjectWithDefault = TypeSpec.objectBuilder("$tracker")
                    .addModifiers(KModifier.PRIVATE)
                    .superclass(
                        SerializerWithDefault::class
                            .asClassName()
                            .parameterizedBy(serializingObject.type)
                    )
                    .addSuperclassConstructorParameter(CodeBlock.of("%N, %T.default", serializingObject.definition, defaultProvider.toClassName()))
                    .build()

                SerializingHierarchy.Placeholder(
                    serializingObject,
                    serializingObjectWithDefault
                )
            } else {
                serializingObject
            }
        }
    }
}

/**
 * Attempt to build a serializing object for the type as if this type is owned,
 * i.e., annotated with @ZKP at its declaration
 * ```
 * @ZKP
 * class MyClass()
 *
 * @ZKP
 * class MyOtherClass(
 *     val myClass: MyClass
 * )
 *     ```
 */
internal fun KSType.trySerializeAsOwnClass(tracker: Tracker): SerializingHierarchy.OfType? {
    if (!this.declaration.isAnnotationPresent(ZKP::class)) {
        return null
    }
    val surrogate = (this.declaration as KSClassDeclaration)
    val surrogateSerializer = surrogate.getSurrogateSerializerClassName()

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            WrappedFixedLengthKSerializer::class
                .asClassName()
                .parameterizedBy(surrogate.toClassName())
        )
        .addSuperclassConstructorParameter(
            CodeBlock.of("%T, %T::class.java.isEnum", surrogateSerializer, surrogate.toClassName())
        )
        .build()

    return SerializingHierarchy.OfType(
        this.toClassName(),
        emptyList(),
        serializingObject
    )
}

/**
 * Attempt to build a serializing object for the type as if this type is a third-party class with a specified surrogate
 * ```
 * class Class3rdParty()
 *
 * @ZKPSurrogate(ConverterFormClass3rdPartToMySurrogateOfClass3rdParty::class)
 * class MySurrogateOfClass3rdParty(): Surrogate<Class3rdParty> { .. }
 *
 * @ZKP
 * class MyClass(
 *     val class3rdParty: Via<MySurrogateOfClass3rdParty::class> Class3rdParty
 *     )
 * ```
 */
internal fun KSType.trySerializeAs3rdPartyClass(tracker: Tracker): SerializingHierarchy.OfType? {
    // Be extra cautious to not enforce order in which the type's serializability is evaluated and return null
    // if @Via annotation is not found.
    // i.e., first, as own class, then as 3rd party class vs first, as 3rd party class, then as own class.

    val viaAnnotation = try {
        this.getNonRepeatableAnnotationByType(Via::class)
    } catch (e: Exception) {
        return null
    }

    val surrogate = viaAnnotation.getSurrogateFromViaAnnotation()
    val target = surrogate.getSurrogateTargetClass()
    val surrogateSerializer = target.getSurrogateSerializerClassName()

    val serializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            WrappedFixedLengthKSerializer::class
                .asClassName()
                .parameterizedBy(target.toClassName())
        )
        .addSuperclassConstructorParameter(
            CodeBlock.of("%T, %T::class.java.isEnum", surrogateSerializer, surrogate.toClassName())
        )
        .build()

    return SerializingHierarchy.OfType(
        this.toClassName(),
        emptyList(),
        serializingObject
    )
}
