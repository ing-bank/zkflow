package com.ing.zkflow.ksp

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import kotlin.reflect.KClass

/**
 * Implementation of [KSAbstractVisitor] that looks for implementations of [interfaceClasses].
 */
class ImplementationsVisitor(
    private val interfaceClasses: List<KClass<*>>
) : KSAbstractVisitor<ScopedDeclaration?, List<Pair<KClass<*>, ScopedDeclaration>>>() {
    override fun defaultVisit(annotated: KSNode, data: ScopedDeclaration?) = emptyList<Pair<KClass<*>, ScopedDeclaration>>()

    override fun visitFile(file: KSFile, data: ScopedDeclaration?) =
        visitDeclarationSequence(file.declarations, data)

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: ScopedDeclaration?
    ): List<Pair<KClass<*>, ScopedDeclaration>> {
        val scopedDeclaration = ScopedDeclaration(data, classDeclaration)

        val implementations = classDeclaration.getAllSuperTypes().mapNotNull { superType ->
            interfaceClasses.find { interfaceClass ->
                interfaceClass.qualifiedName == superType.declaration.qualifiedName?.asString()
            }?.let {
                Pair(it, scopedDeclaration)
            }
        }.toList()

        return implementations +
            visitDeclarationSequence(classDeclaration.declarations, scopedDeclaration)
    }

    private fun visitDeclarationSequence(declarations: Sequence<KSDeclaration>, data: ScopedDeclaration?): List<Pair<KClass<*>, ScopedDeclaration>> {
        return declarations
            .filterIsInstance<KSClassDeclaration>()
            .flatMap { visitClassDeclaration(it, data) }
            .toList()
    }

    companion object {
        /**
         * Convenience function to convert from a list of pairs to a map with a list.
         */
        fun List<Pair<KClass<*>, ScopedDeclaration>>.toMapOfLists(): Map<KClass<*>, List<ScopedDeclaration>> {
            return this.fold(mutableMapOf()) { acc, (interfaceClass, implementation) ->
                val implementations = acc[interfaceClass]?.let {
                    it + implementation
                } ?: listOf(implementation)
                acc[interfaceClass] = implementations
                acc
            }
        }
    }
}

/**
 * Utility class to generate correct java and kotlin names for nested [KSClassDeclaration].
 */
data class ScopedDeclaration(
    val parent: ScopedDeclaration?,
    val declaration: KSClassDeclaration,
) : HasQualifiedName {
    val java: HasQualifiedName = object : HasQualifiedName {
        override val qualifiedName: String by lazy {
            parent?.let {
                "${it.java.qualifiedName}$${declaration.simpleName.asString()}"
            } ?: "${declaration.packageName.asString()}.${declaration.simpleName.asString()}"
        }
    }

    override val qualifiedName: String = parent?.let { "${it.qualifiedName}.${declaration.simpleName.asString()}" }
        ?: "${declaration.packageName.asString()}.${declaration.simpleName.asString()}"
}

interface HasQualifiedName {
    val qualifiedName: String
}
