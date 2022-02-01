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
    private val interfaceClasses: List<KClass<*>>
) : KSAbstractVisitor<ScopedDeclaration?, Map<KClass<*>, List<ScopedDeclaration>>>() {
    override fun defaultVisit(annotated: KSNode, data: ScopedDeclaration?) = emptyMap<KClass<*>, List<ScopedDeclaration>>()

    override fun visitFile(file: KSFile, data: ScopedDeclaration?) =
        visitDeclarationSequence(file.declarations, data)

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: ScopedDeclaration?
    ): Map<KClass<*>, List<ScopedDeclaration>> {
        val scopedDeclaration = ScopedDeclaration(data, classDeclaration)

        val implementations = classDeclaration.getAllSuperTypes().mapNotNull { superType ->
            interfaceClasses.find { interfaceClass ->
                interfaceClass.qualifiedName == superType.declaration.qualifiedName?.asString()
            }?.let {
                Pair(it, listOf(scopedDeclaration))
            }
        }.toMap()

        return implementations.merge(visitDeclarationSequence(classDeclaration.declarations, scopedDeclaration))
    }

    private fun visitDeclarationSequence(declarations: Sequence<KSDeclaration>, data: ScopedDeclaration?): Map<KClass<*>, List<ScopedDeclaration>> =
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
            } ?: "${declaration.packageName.asString()}.${declaration.simpleName.asString()}"
        }
    }

    override val qualifiedName: String = parent?.let { "${it.qualifiedName}.${declaration.simpleName.asString()}" }
        ?: "${declaration.packageName.asString()}.${declaration.simpleName.asString()}"
}

interface HasQualifiedName {
    val qualifiedName: String
}
