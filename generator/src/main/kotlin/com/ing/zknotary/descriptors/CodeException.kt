package com.ing.zknotary.descriptors

import com.google.devtools.ksp.symbol.KSType
import java.lang.Exception
import kotlin.reflect.KClass

sealed class CodeException(message: String) : Exception(message) {
    class InvalidDeclaration(type: KSType) : CodeException(
        "${type.declaration} is incorrectly defined: ${type.declaration.location}."
    )

    class MissingAnnotation(type: KSType, annotationClazz: KClass<*>) : CodeException(
        "$type is expected to be annotated with `${annotationClazz.simpleName}`."
    )

    class InvalidAnnotation(annotationClazz: KClass<*>, expectedArgument: String) : CodeException(
        "${annotationClazz.simpleName} is expected to have argument: `$expectedArgument`."
    )

    class InvalidAnnotationArgument(annotationClazz: KClass<*>, expectedArgument: String) : CodeException(
        "${annotationClazz.simpleName} incorrectly defines argument: `$expectedArgument`."
    )

    class NotAClass(types: List<String>) : CodeException(
        "Types [ ${types.joinToString(separator = ", ") } ] cannot be instantiated."
    ) {
        constructor(type: String) : this(listOf(type))
    }

    class DefaultConstructorAbsent(type: KSType) : CodeException(
        "Type $type is supposed to have an empty constructor but it does not."
    )
}
