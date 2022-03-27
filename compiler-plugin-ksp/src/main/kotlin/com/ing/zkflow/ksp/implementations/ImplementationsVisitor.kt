package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.ing.zkflow.ksp.KSAbstractVisitor
import com.ing.zkflow.util.merge
import kotlin.reflect.KClass

/**
 * Implementation of [KSAbstractVisitor] that looks for implementations of [interfaceClasses].
 */
class ImplementationsVisitor(
    private val interfaceClasses: List<Set<KClass<*>>>
) : KSAbstractVisitor<ScopedDeclaration?, Map<Set<KClass<*>>, List<ScopedDeclaration>>>() {
    override fun defaultVisit(annotated: KSNode, data: ScopedDeclaration?) = emptyMap<Set<KClass<*>>, List<ScopedDeclaration>>()

    override fun visitFile(file: KSFile, data: ScopedDeclaration?) =
        visitDeclarationSequence(file.declarations, data)

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: ScopedDeclaration?
    ): Map<Set<KClass<*>>, List<ScopedDeclaration>> {
        val scopedDeclaration = ScopedDeclaration(data, classDeclaration)

        val superTypesSet = classDeclaration.getAllSuperTypes().map { it.declaration.qualifiedName?.asString() }.toSet()
        val matches = interfaceClasses.filter { interfaceSet -> superTypesSet.containsAll(interfaceSet.map { it.qualifiedName }) }

        val implementations = if (matches.isNotEmpty()) {
            matches.fold(mutableMapOf<Set<KClass<*>>, List<ScopedDeclaration>>()) { acc, match ->
                acc[match] = listOf(scopedDeclaration)
                acc
            }
        } else {
            emptyMap()
        }
        return implementations.merge(visitDeclarationSequence(classDeclaration.declarations, scopedDeclaration))
    }

    private fun visitDeclarationSequence(declarations: Sequence<KSDeclaration>, data: ScopedDeclaration?): Map<Set<KClass<*>>, List<ScopedDeclaration>> =
        declarations
            .filterIsInstance<KSClassDeclaration>()
            .fold(emptyMap()) { acc, file -> acc.merge(visitClassDeclaration(file, data)) }
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
            } ?: buildQualifiedName(declaration)
        }
    }

    override val qualifiedName: String = parent?.let {
        "${it.qualifiedName}.${declaration.simpleName.asString()}"
    } ?: buildQualifiedName(declaration)

    private fun buildQualifiedName(declaration: KSClassDeclaration): String {
        val packageName = if (declaration.packageName.asString().isNotBlank()) {
            "${declaration.packageName.asString()}."
        } else {
            ""
        }

        return "$packageName${declaration.simpleName.asString()}"
    }
}

interface HasQualifiedName {
    val qualifiedName: String
}
