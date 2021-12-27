package com.ing.zkflow.plugins.serialization

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

sealed class BestEffortResolvedType {
    data class AsIs(val simpleName: String) : BestEffortResolvedType() {
        override fun asString() = simpleName
        override fun toString() = "AsIs($simpleName)"
    }

    data class FullyQualified(val fqName: FqName, val annotations: List<KtAnnotationEntry>) : BestEffortResolvedType() {
        override fun asString() = "$fqName"
        override fun toString() = "FullyQualified($fqName, [${annotations.joinToString(separator = ", ") { it.text }}])"

        /**
         * Finds _first_ annotation of type T.
         */
        inline fun <reified T : Any> findAnnotation() = annotations.firstOrNull { "${it.shortName}" == T::class.simpleName }
    }

    data class FullyResolved(val kClass: KClass<*>) : BestEffortResolvedType() {
        val fqName = FqName.fromSegments(kClass.qualifiedName!!.split("."))
        override fun asString() = "$fqName"
        override fun toString() = "FullyResolved($fqName, [${kClass.annotations.joinToString(separator = ", ") { "$it" }}])"

        /**
         * Finds _first_ annotation of type T.
         */
        inline fun <reified T : Annotation> findAnnotation() = kClass.findAnnotation<T>()
    }

    abstract fun asString(): String
}
