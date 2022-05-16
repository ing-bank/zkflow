package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.ing.zkflow.ksp.KSAbstractVisitor
import com.ing.zkflow.util.merge
import kotlin.reflect.KClass

/**
 * Implementation of [KSAbstractVisitor] that looks for implementations of [interfaceClasses].
 * @param interfaceClasses: each element of the list is a set of interfaces to look for. The visitor will look for
 *  declarations which implement ALL of the interfaces in the set.
 */
class ImplementationsVisitor(
    private val implementors: List<Implementor>
) : KSAbstractVisitor<ScopedDeclaration?, Map<Implementor, List<ScopedDeclaration>>>() {
    override fun defaultVisit(annotated: KSNode, data: ScopedDeclaration?) = emptyMap<Implementor, List<ScopedDeclaration>>()

    override fun visitFile(file: KSFile, data: ScopedDeclaration?) =
        visitDeclarationSequence(file.declarations, data)

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: ScopedDeclaration?
    ): Map<Implementor, List<ScopedDeclaration>> {
        val scopedDeclaration = ScopedDeclaration(data, classDeclaration)

        val matches = selectImplementors(classDeclaration)

        val implementations = if (matches.isNotEmpty()) {
            matches.fold(mutableMapOf<Implementor, List<ScopedDeclaration>>()) { acc, match ->
                acc[match] = listOf(scopedDeclaration)
                acc
            }
        } else {
            emptyMap()
        }

        return implementations.merge(visitDeclarationSequence(classDeclaration.declarations, scopedDeclaration))
    }

    private fun selectImplementors(classDeclaration: KSClassDeclaration): List<Implementor> {
        val superTypes = classDeclaration.getAllSuperTypes().map { it.declaration.qualifiedName?.asString() }.toSet()

        return implementors.filter { implementor ->
            // If a requirement on the class kind is set,
            // check if `classDeclaration` is of the right kind.
            implementor.possibleClassKinds?.let { possibleClassKinds ->
                if (classDeclaration.classKind !in possibleClassKinds) {
                    return@filter false
                }
            }

            val requiredTypes = implementor.superTypes.map { it.qualifiedName }

            superTypes.containsAll(requiredTypes)
        }
    }

    private fun visitDeclarationSequence(declarations: Sequence<KSDeclaration>, data: ScopedDeclaration?): Map<Implementor, List<ScopedDeclaration>> =
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

data class Implementor(
    /**
     * *ALL* required types to implement or extend from.
     */
    val superTypes: Set<KClass<*>>,

    /**
     * Implementor must be one of `anyClassKind`, if set.
     */
    val possibleClassKinds: Set<ClassKind>? = null
) {

    companion object {
        fun isInterface(superTypes: Set<KClass<*>>) =
            Implementor(superTypes, setOf(ClassKind.INTERFACE))

        fun isClassOrObject(superTypes: Set<KClass<*>>) =
            Implementor(superTypes, setOf(ClassKind.CLASS, ClassKind.OBJECT))
    }
}
