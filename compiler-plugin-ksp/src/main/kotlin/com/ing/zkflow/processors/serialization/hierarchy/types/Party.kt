package com.ing.zkflow.processors.serialization.hierarchy.types

import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.ing.zkflow.annotations.corda.CordaX500NameSpec
import com.ing.zkflow.annotations.corda.SignatureSpec
import com.ing.zkflow.ksp.getNonRepeatableAnnotationByType
import com.ing.zkflow.ksp.getSingleMetaAnnotationByType
import com.ing.zkflow.ksp.getSurrogateSerializerClassName
import com.ing.zkflow.processors.serialization.hierarchy.SerializingHierarchy
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer
import com.ing.zkflow.serialization.serializer.corda.PartySerializer
import com.ing.zkflow.tracking.Tracker
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import net.corda.core.identity.CordaX500Name

@Suppress("LongMethod")
internal fun KSTypeReference.asParty(tracker: Tracker): SerializingHierarchy {
    val type = resolve()

    val cordaSignatureId = getSingleMetaAnnotationByType(SignatureSpec::class)
        .arguments
        .single()
        .value!!
        .toString()
        .toInt()

    val cordaX500NameSerializingHierarchy = if (isAnnotationPresent(CordaX500NameSpec::class)) {
        val cordaX500NameSurrogate = getNonRepeatableAnnotationByType(CordaX500NameSpec::class)
            .annotationType
            .resolve()
            .arguments
            .single()
            .type!!
            .resolve()

        val cordaX500NameSerializer = (cordaX500NameSurrogate.declaration as KSClassDeclaration).getSurrogateSerializerClassName()

        val serializingObject = TypeSpec.objectBuilder("${tracker.next()}")
            .addModifiers(KModifier.PRIVATE)
            .superclass(
                SerializerWithDefault::class
                    .asClassName()
                    .parameterizedBy(CordaX500Name::class.asClassName())
            )
            .addSuperclassConstructorParameter(
                CodeBlock.of("%T, %T.default", cordaX500NameSerializer, CordaX500NameSerializer::class)
            )
            .build()

        SerializingHierarchy.OfType(
            CordaX500Name::class.asClassName(),
            emptyList(),
            serializingObject,
            isParameterizing = false
        )
    } else {
        val serializingObject = TypeSpec.objectBuilder("${tracker.next()}")
            .addModifiers(KModifier.PRIVATE)
            .superclass(
                WrappedFixedLengthKSerializerWithDefault::class
                    .asClassName()
                    .parameterizedBy(CordaX500Name::class.asClassName())
            )
            .addSuperclassConstructorParameter(
                CodeBlock.of("%T", CordaX500NameSerializer::class)
            )
            .build()

        SerializingHierarchy.OfType(
            CordaX500Name::class.asClassName(),
            emptyList(),
            serializingObject,
            isParameterizing = false
        )
    }

    val partySerializingObject = TypeSpec.objectBuilder("$tracker")
        .addModifiers(KModifier.PRIVATE)
        .superclass(PartySerializer::class.asClassName())
        .addSuperclassConstructorParameter(
            CodeBlock.of("%L, %N", cordaSignatureId, cordaX500NameSerializingHierarchy.definition)
        )
        .build()

    return SerializingHierarchy.OfType(
        type.toClassName(),
        listOf(cordaX500NameSerializingHierarchy),
        partySerializingObject
    )
}
