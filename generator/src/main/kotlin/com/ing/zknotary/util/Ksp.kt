@file:Suppress("TooManyFunctions")

package com.ing.zknotary.util

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.STAR as KpStar

// --> 3rd party methods.
// https://www.zacsweers.dev/kotlin-symbol-processor-early-thoughts/
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
// <-- 3rd party methods

inline fun <reified T> KSType.findAnnotation(): KSAnnotation? =
    annotations.find {
        it.annotationType.toString().equals(
            T::class.simpleName ?: error("Unknown annotation class is expected"),
            ignoreCase = true
        )
    }

inline fun <reified T> KSAnnotation.findArgument(name: String): T? =
    arguments.single {
        it.name?.getShortName() == name
    }.value as? T

inline fun <reified T> KSAnnotation.getArgumentOrDefault(name: String, default: T): T =
    arguments.single {
        it.name?.getShortName() == name
    }.value as? T ?: default

val KSClassDeclaration.sizedName: String
    get() = "${simpleName.asString()}Sized"

val KSDeclaration.asClassName: ClassName
    get() = ClassName(
        this.packageName.asString(),
        listOf(this.simpleName.asString())
    )
