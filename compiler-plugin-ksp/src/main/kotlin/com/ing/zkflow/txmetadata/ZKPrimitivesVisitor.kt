package com.ing.zkflow.txmetadata

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.ksp.KSAbstractVisitor

class ZKPrimitivesVisitor : KSAbstractVisitor<ScopedDeclaration?, List<ZKPrimitive>>() {
    override fun defaultVisit(annotated: KSNode, data: ScopedDeclaration?) = emptyList<ZKPrimitive>()

    override fun visitFile(file: KSFile, data: ScopedDeclaration?) =
        visitDeclarationSequence(file.declarations, data)

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: ScopedDeclaration?): List<ZKPrimitive> {
        val scopedDeclaration = ScopedDeclaration(data, classDeclaration)

        return listOfNotNull(scopedDeclaration.toZKPrimitive()) +
            visitDeclarationSequence(classDeclaration.declarations, scopedDeclaration)
    }

    private fun visitDeclarationSequence(declarations: Sequence<KSDeclaration>, data: ScopedDeclaration?): List<ZKPrimitive> {
        return declarations
            .filterIsInstance<KSClassDeclaration>()
            .flatMap { visitClassDeclaration(it, data) }
            .toList()
    }
}

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

    fun toZKPrimitive(): ZKPrimitive? = declaration.getAllSuperTypes().run {
        when {
            any { ZKOwnableState::class.qualifiedName == it.declaration.qualifiedName?.asString() } ->
                ZKPrimitive.State(this@ScopedDeclaration)
            any { ZKTransactionMetadataCommandData::class.qualifiedName == it.declaration.qualifiedName?.asString() } ->
                ZKPrimitive.Command(this@ScopedDeclaration)
            else -> null
        }
    }
}

interface HasQualifiedName {
    val qualifiedName: String
}

sealed class ZKPrimitive(
    val declaration: ScopedDeclaration
) {
    class Command(declaration: ScopedDeclaration) : ZKPrimitive(declaration)
    class State(declaration: ScopedDeclaration) : ZKPrimitive(declaration)
}
