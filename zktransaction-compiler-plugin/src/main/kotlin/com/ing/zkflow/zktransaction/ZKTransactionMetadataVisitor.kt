package com.ing.zkflow.zktransaction

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData

class ZKTransactionMetadataVisitor : KSVisitorVoid() {
    internal val zkTransactionMetadataClasses = mutableListOf<KSClassDeclaration>()

    override fun visitFile(file: KSFile, data: Unit) {
        visitDeclarationSequence(file.declarations, data)
    }

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.implementsZKTransactionMetadataCommandData()) {
            zkTransactionMetadataClasses.add(classDeclaration)
        }
        visitDeclarationSequence(classDeclaration.declarations, data)
    }

    private fun visitDeclarationSequence(declarations: Sequence<KSDeclaration>, data: Unit) {
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
