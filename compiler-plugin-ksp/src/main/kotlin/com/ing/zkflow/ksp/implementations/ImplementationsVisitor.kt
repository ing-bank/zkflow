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
 * Implementation of [KSAbstractVisitor] that looks for declarations satisfying [implementationRequirements].
 * @param implementationRequirements: each element of the list is a requirement on a class kind and implemented/extended interfaces/classes.
 * The visitor will look for declarations matching the required class kind and which implement ALL of the interfaces/classes specified by a respective instance of [ImplementationRequirement].
 */
class ImplementationsVisitor(
    private val implementationRequirements: List<ImplementationRequirement>
) : KSAbstractVisitor<ScopedDeclaration?, Map<ImplementationRequirement, List<ScopedDeclaration>>>() {
    override fun defaultVisit(annotated: KSNode, data: ScopedDeclaration?) = emptyMap<ImplementationRequirement, List<ScopedDeclaration>>()

    override fun visitFile(file: KSFile, data: ScopedDeclaration?) =
        visitDeclarationSequence(file.declarations, data)

    override fun visitClassDeclaration(
        classDeclaration: KSClassDeclaration,
        data: ScopedDeclaration?
    ): Map<ImplementationRequirement, List<ScopedDeclaration>> {
        val scopedDeclaration = ScopedDeclaration(data, classDeclaration)

        val matches = selectImplementors(classDeclaration)

        val implementations = if (matches.isNotEmpty()) {
            matches.fold(mutableMapOf<ImplementationRequirement, List<ScopedDeclaration>>()) { acc, match ->
                acc[match] = listOf(scopedDeclaration)
                acc
            }
        } else {
            emptyMap()
        }

        return implementations.merge(visitDeclarationSequence(classDeclaration.declarations, scopedDeclaration))
    }

    /**
     * Given a [classDeclaration], function selects class declarations of the kind required by [implementationRequirements]
     * and implementing/extending all of the interface/classes of the respective implementation requirement.
     */
    private fun selectImplementors(classDeclaration: KSClassDeclaration): List<ImplementationRequirement> {
        val superTypes = classDeclaration.getAllSuperTypes().map { it.declaration.qualifiedName?.asString() }.toSet()

        return implementationRequirements.filter { implementationRequirement ->
            // If a requirement on the class kind is set,
            // check if `classDeclaration` is of the right kind.
            implementationRequirement.possibleClassKinds?.let { possibleClassKinds ->
                if (classDeclaration.classKind !in possibleClassKinds) {
                    return@filter false
                }
            }

            // Verify that all required types are implemented by this class declaration.
            val requiredTypes = implementationRequirement.superTypes.map { it.qualifiedName }

            superTypes.containsAll(requiredTypes)
        }
    }

    private fun visitDeclarationSequence(declarations: Sequence<KSDeclaration>, data: ScopedDeclaration?): Map<ImplementationRequirement, List<ScopedDeclaration>> =
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

data class ImplementationRequirement(
    /**
     * *ALL* required types to implement or extend from.
     */
    val superTypes: Set<KClass<*>>,

    /**
     * ImplementationRequirement must be one of `anyClassKind`, if set.
     */
    val possibleClassKinds: Set<ClassKind>? = null
) {

    companion object {
        fun isInterface(superTypes: Set<KClass<*>>) =
            ImplementationRequirement(superTypes, setOf(ClassKind.INTERFACE))

        fun isClassOrObject(superTypes: Set<KClass<*>>) =
            ImplementationRequirement(superTypes, setOf(ClassKind.CLASS, ClassKind.OBJECT))
    }
}
