@file:Suppress("TooManyFunctions")
package com.ing.zknotary.util

import com.google.devtools.ksp.processing.KSPLogger

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.annotations.WrappedList
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlin.math.log
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.descriptors.PossiblyInnerType
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.STAR as KpStar

inline fun <reified T> Resolver.getClassDeclarationByName(): KSClassDeclaration {
    return getClassDeclarationByName(T::class.qualifiedName!!)
}

fun Resolver.getClassDeclarationByName(fqcn: String): KSClassDeclaration {
    return getClassDeclarationByName(getKSNameFromString(fqcn)) ?: error("Class '$fqcn' not found.")
}

fun KSClassDeclaration.asType() = asType(emptyList())

fun KSAnnotated.getAnnotationWithType(target: KSType): KSAnnotation {
    return findAnnotationWithType(target) ?: error("Annotation $target not found.")
}

fun KSAnnotated.hasAnnotation(target: KSType): Boolean {
    return findAnnotationWithType(target) != null
}

fun KSAnnotated.findAnnotationWithType(target: KSType): KSAnnotation? {
    return annotations.find { it.annotationType.resolve() == target }
}

inline fun <reified T> KSAnnotation.getMember(name: String): T {
    return arguments.find { it.name?.getShortName() == name }
        ?.value as? T
        ?: error("No member name found for $name.")
}

// this seems to fail
fun KSType.toTypeName(): TypeName {
    val type = when (declaration) {
        is KSClassDeclaration -> {
            (this as KSClassDeclaration).toTypeName(typeParameters.map { it.toTypeName() })
        }
        is KSTypeParameter -> {
            (this as KSTypeParameter).toTypeName()
        }
        else -> error("Unsupported type: $declaration")
    }

    val nullable = nullability == Nullability.NULLABLE

    return type.copy(nullable = nullable)
}

fun KSClassDeclaration.toTypeName(
    actualTypeArgs: List<TypeName> = typeParameters.map { it.toTypeName() }
): TypeName {
    val className = toClassName()
    return if (typeParameters.isNotEmpty()) {
        className.parameterizedBy(actualTypeArgs)
    } else {
        className
    }
}

fun KSClassDeclaration.toClassName(): ClassName {
    // Not ideal to be using bestGuess - https://github.com/android/kotlin/issues/23
    return ClassName.bestGuess(qualifiedName!!.asString())
}

fun KSTypeParameter.toTypeName(): TypeName {
    if (variance == Variance.STAR) return KpStar
    val typeVarName = name.getShortName()
    val typeVarBounds = bounds.map { it.toTypeName() }
    val typeVarVariance = when (variance) {
        Variance.COVARIANT -> KModifier.IN
        Variance.CONTRAVARIANT -> KModifier.OUT
        else -> null
    }
    return TypeVariableName(typeVarName, bounds = typeVarBounds, variance = typeVarVariance)
}

fun KSTypeReference.toTypeName(): TypeName {
    val type = resolve()
    return type.toTypeName()
}

fun KSAnnotation.toTypeName(): TypeName {
    return annotationType.resolve().toTypeName()
}

///
val KSType.typeName: TypeName
    get() {
        val primaryType = declaration.toString()
        val typeArgs = arguments.mapNotNull { it.type?.resolve() }

        val clazzName = ClassName(
            declaration.packageName.asString(),
            listOf(primaryType)
        )

        return if (typeArgs.isNotEmpty()) {
            clazzName.parameterizedBy(typeArgs.map { it.typeName })
        } else {
            clazzName
        }
    }
