@file:Suppress("TooManyFunctions")

package com.ing.zkflow.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.annotations.corda.Algorithm
import com.ing.zkflow.annotations.corda.SignatureSpec
import com.ing.zkflow.util.buildFullyDistinguishableClassName
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlinx.serialization.Contextual
import kotlin.reflect.KClass

private val implementedInterfacesCache: MutableMap<KSClassDeclaration, Sequence<KSClassDeclaration>> = mutableMapOf()
private val implementedDirectInterfacesCache: MutableMap<KSClassDeclaration, Sequence<KSClassDeclaration>> = mutableMapOf()

fun KSClassDeclaration.getAllImplementedInterfaces(): Sequence<KSClassDeclaration> = implementedInterfacesCache.getOrPut(this) {
    getAllSuperTypes()
        .map { it.declaration }
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.INTERFACE }
}

fun KSClassDeclaration.getAllDirectlyImplementedInterfaces(): Sequence<KSClassDeclaration> =
    implementedDirectInterfacesCache.getOrPut(this) {
        superTypes
            .map { it.resolve().declaration }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.INTERFACE }
    }

/**
 * Returns true if the receiver implements the interfaceClass either directly or indirectly
 */
fun KSClassDeclaration.implementsInterface(interfaceClass: KClass<*>): Boolean =
    getAllImplementedInterfaces().any {
        it.qualifiedName?.asString() == interfaceClass.qualifiedName
    }

/**
 * Returns true only if the receiver implements the interfaceClass directly
 */
fun KSClassDeclaration.implementsInterfaceDirectly(interfaceClass: KClass<*>): Boolean =
    getAllDirectlyImplementedInterfaces().any {
        it.qualifiedName?.asString() == interfaceClass.qualifiedName
    }

/**
 * Returns true only if the receiver implements the interfaceClass directly
 */
fun KSClassDeclaration.implementsInterfaceDirectly(interfaceClass: KSClassDeclaration): Boolean =
    getAllDirectlyImplementedInterfaces().any {
        it.qualifiedName?.asString() == interfaceClass.qualifiedName?.asString()
    }

fun KSAnnotated.getAnnotationsByType(annotationKClass: KClass<out Annotation>): Sequence<KSAnnotation> {
    return this.annotations.filter {
        it.shortName.getShortName() == annotationKClass.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationKClass.qualifiedName
    }
}

fun KSClassDeclaration.getSurrogateTargetClass(): KSClassDeclaration {
    val type = getSurrogateTargetType()
    return type
        .declaration as? KSClassDeclaration
        ?: error("$type is not a KSClassDeclaration.")
}

fun KSClassDeclaration.getSurrogateTargetType(): KSType {
    val surrogateArgument = superTypes.single {
        it.resolve().declaration.qualifiedName?.asString() == Surrogate::class.qualifiedName
    }.resolve().arguments.single()

    return surrogateArgument
        .type
        ?.resolve()
        ?: error("Could not resolve type of $surrogateArgument.")
}

fun Sequence<KSClassDeclaration>.filterConcreteClassesOrObjects(): Sequence<KSClassDeclaration> =
    filter { it.classKind in listOf(ClassKind.ENUM_CLASS, ClassKind.CLASS, ClassKind.OBJECT) && !it.isAbstract() }

/**
 * Surrogates are classes that are annotated with @ZKPSurrogate *and* implement [Surrogate].
 * Classes with only the annotation cause an error, classes only implementing the interface will be ignored.
 */
fun Resolver.getAllSurrogates(): Sequence<KSClassDeclaration> {
    val annotated = this.findClassesOrObjectsWithAnnotation(ZKPSurrogate::class)
    val implementing = annotated.filter { it.implementsInterface(Surrogate::class) }

    require(annotated.count() == implementing.count()) {
        "All @${ZKPSurrogate::class.simpleName}-annotated classes should implement the ${Surrogate::class.simpleName} interface. " +
            "The following do not: ${(annotated - implementing).map { it.qualifiedName?.asString() }.joinToString(", ")}"
    }
    return implementing
}

fun Resolver.findClassesOrObjectsWithAnnotation(annotationKClass: KClass<out Annotation>): Sequence<KSClassDeclaration> {
    return getSymbolsWithAnnotation(annotationKClass.qualifiedName!!)
        .filterIsInstance<KSClassDeclaration>()
        .filterConcreteClassesOrObjects()
}

fun KSAnnotated.getSingleArgumentOfSingleAnnotationByType(annotationKClass: KClass<out Annotation>): Any {
    return this.getAnnotationsByType(annotationKClass).single().arguments.single().value!!
}

fun KSTypeReference.getNonRepeatableAnnotationByType(annotationKClass: KClass<out Annotation>): KSAnnotation {
    return annotations
        .single {
            it.shortName.getShortName() == annotationKClass.simpleName &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == annotationKClass.qualifiedName
        }
}

fun KSTypeReference.getCordaSignatureId(): Int {
    return getSingleMetaAnnotationByType(SignatureSpec::class)
        .arguments
        .single()
        .value!!
        .toString()
        .toInt()
}

fun KSTypeReference.getDigestAlgorithm(): KSType {
    return getSingleMetaAnnotationByType(Algorithm::class)
        .arguments
        .single()
        .value!! as KSType
}

/**
 * Inspects every annotation (let's call it) `A` of a [KSType] and selects
 * an [annotationKClass] of `A`, e.g., in the following piece
 *
 * ```
 * val value: @A @B Int
 *
 * @[annotationKClass]
 * annotation class A
 *
 * annotation class B
 * ```
 *
 * @[annotationKClass] will be selected.
 */
fun KSTypeReference.getSingleMetaAnnotationByType(annotationKClass: KClass<out Annotation>): KSAnnotation {
    return annotations.mapNotNull {
        try {
            it.annotationType.resolve().declaration.getAnnotationsByType(annotationKClass).single()
        } catch (_: Exception) {
            null
        }
    }.single()
}

fun KSClassDeclaration.getSurrogateClassName(isEnum: Boolean): ClassName {
    return if (isEnum) {
        toClassName().buildFullyDistinguishableClassName(
            Surrogate.GENERATED_SURROGATE_POSTFIX,
            Surrogate.GENERATED_SURROGATE_ENUM_POSTFIX
        )
    } else {
        toClassName().buildFullyDistinguishableClassName(
            Surrogate.GENERATED_SURROGATE_POSTFIX
        )
    }
}

fun KSClassDeclaration.getSurrogateSerializerClassName(): ClassName {
    return toClassName().buildFullyDistinguishableClassName(Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX)
}

fun KSAnnotation.getSurrogateFromViaAnnotation(): KSClassDeclaration {
    require(annotationType.resolve().declaration.qualifiedName?.asString() == Via::class.qualifiedName) {
        "Can't get Surrogate from $this, it is not a @${Via::class.simpleName} annotation."
    }
    return annotationType.element?.typeArguments?.singleOrNull()?.type?.resolve()?.declaration as? KSClassDeclaration
        ?: error("@${Via::class.simpleName} annotation's argument is not a class.")
}

/**
 * Prepends every type with @[Contextual] annotation.
 */
fun KSType.toContextualTypeName() =
    toTypeNameWithAnnotation(listOf(Contextual::class))

/**
 * Removes all annotations from the type declaration.
 */
fun KSType.toCleanTypeName() =
    toTypeNameWithAnnotation(emptyList())

/**
 * Recursively walks the type tree and attaches ALL given annotations on ALL levels.
 */
private fun KSType.toTypeNameWithAnnotation(annotations: List<KClass<out Annotation>>): TypeName {
    if (arguments.isEmpty()) {
        return toClassName()
            .copy(
                annotations = annotations.map {
                    AnnotationSpec.builder(it.asClassName()).build()
                },
                nullable = isMarkedNullable
            )
    }

    val typeArguments = arguments.map { it.type!!.resolve().toTypeNameWithAnnotation(annotations) }

    return toClassName()
        .parameterizedBy(typeArguments)
        .copy(
            annotations = annotations.map {
                AnnotationSpec.builder(it.asClassName()).build()
            },
            nullable = isMarkedNullable
        )
}
