package com.ing.zkflow.txmetadata

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.ksp.KSAbstractVisitor

data class ScopedDeclaration(
    val parent: ScopedDeclaration?,
    val declaration: KSClassDeclaration,
) {
    fun qualifiedName(): String {
        return parent?.let {
            "${parent.qualifiedName()}$${declaration.simpleName.asString()}"
        } ?: "${declaration.packageName.asString()}.${declaration.simpleName.asString()}"
    }
}

class ZKTransactionMetadataVisitor : KSAbstractVisitor<ScopedDeclaration?, Unit>() {
    internal val zkTransactionMetadataClasses = mutableListOf<ScopedDeclaration>()

    override fun defaultVisit(annotated: KSNode, data: ScopedDeclaration?) {
        return
    }

    override fun visitFile(file: KSFile, data: ScopedDeclaration?) {
        visitDeclarationSequence(file.declarations, data)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: ScopedDeclaration?) {
        val scopedDeclaration = ScopedDeclaration(data, classDeclaration)
        if (classDeclaration.implementsZKTransactionMetadataCommandData()) {
            zkTransactionMetadataClasses.add(scopedDeclaration)
        }
        visitDeclarationSequence(classDeclaration.declarations, scopedDeclaration)
    }

    private fun visitDeclarationSequence(declarations: Sequence<KSDeclaration>, data: ScopedDeclaration?) {
        declarations
            .filterIsInstance<KSClassDeclaration>()
            .forEach {
                visitClassDeclaration(it, data)
            }
    }

    private fun KSClassDeclaration.implementsZKTransactionMetadataCommandData() =
        getAllSuperTypes()
            .any {
                ZKTransactionMetadataCommandData::class.qualifiedName == it.declaration.qualifiedName?.asString()
            }
}
