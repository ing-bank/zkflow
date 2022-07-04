package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.ing.zkflow.Default
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.ksp.getNonRepeatableAnnotationByType
import com.ing.zkflow.ksp.getSingleArgumentOfNonRepeatableAnnotationByType
import com.ing.zkflow.ksp.getSurrogateFromViaAnnotation
import com.ing.zkflow.ksp.getSurrogateSerializerClassName
import com.ing.zkflow.ksp.getSurrogateTargetClass
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

/** Process type as a user type.
 * User type is serializable if
 * - this type is owned, i.e., annotated with [ZKP]
 * - this type is a third-party class with a specified surrogate, i.e., annotated with [ZKPSurrogate]
 *
 * - Possibly must have a @Default annotation.

 * If `this` must have default, then extra level of indirection is required, therefore
 * surrogate serializer must take the next tracker, i.e.
 * (** Previous recursion level **)    (***********************   Current recursion level    ************************)
 *      NullableSerializer          ->  WrappedKSerializerWithDefault(tracker) -> SurrogateSerializer(tracker.next())
 *                                      ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
 */
internal fun KSTypeReference.asUserType(tracker: Tracker, mustHaveDefault: Boolean): SerializingHierarchy {

    val nextTracker = if (mustHaveDefault) tracker.next() else tracker

    val serializingHierarchy = this.trySerializeAsOwnClass(nextTracker) ?: this.trySerializeAs3rdPartyClass(nextTracker) ?: error("")

    return if (mustHaveDefault) {
        val defaultProvider = this.getSingleArgumentOfNonRepeatableAnnotationByType(Default::class) as KSType

        val serializingObjectWithDefault = TypeSpec.objectBuilder("$tracker")
            .addModifiers(KModifier.PRIVATE)
            .superclass(
                SerializerWithDefault::class
                    .asClassName()
                    .parameterizedBy(serializingHierarchy.type)
            )
            .addSuperclassConstructorParameter(CodeBlock.of("%N, %T.default", serializingHierarchy.definition, defaultProvider.toClassName()))
            .build()

        SerializingHierarchy.OfDefaultable(
            serializingHierarchy,
            serializingObjectWithDefault
        )
    } else {
        serializingHierarchy
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
private fun KSTypeReference.trySerializeAsOwnClass(tracker: Tracker): SerializingHierarchy.OfType? {
    val type = resolve()

    if (!type.declaration.isAnnotationPresent(ZKP::class)) {
        return null
    }
    val surrogate = (type.declaration as KSClassDeclaration)
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
        type.toClassName(),
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
private fun KSTypeReference.trySerializeAs3rdPartyClass(tracker: Tracker): SerializingHierarchy.OfType? {
    val type = resolve()

    // Be extra cautious to not enforce order in which the type's serializability is evaluated and return null
    // if @Via annotation is not found.
    // i.e., first, as own class, then as 3rd party class vs first, as 3rd party class, then as own class.
    val viaAnnotation = try {
        getNonRepeatableAnnotationByType(Via::class)
    } catch (e: Exception) {
        return null
    }

    val surrogate = viaAnnotation.getSurrogateFromViaAnnotation()
    val target = surrogate.getSurrogateTargetClass()
    val surrogateSerializer = surrogate.getSurrogateSerializerClassName()

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
        type.toClassName(),
        emptyList(),
        serializingObject
    )
}
