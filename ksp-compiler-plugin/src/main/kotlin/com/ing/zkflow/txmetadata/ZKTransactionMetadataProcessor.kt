package com.ing.zkflow.txmetadata

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.ksp.KotlinSymbolProcessor
import net.corda.core.internal.packageName
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class ZKTransactionMetadataProcessor(
    private val environment: SymbolProcessorEnvironment
) : KotlinSymbolProcessor {
    override fun process(resolver: Resolver, allKotlinFiles: Sequence<KSFile>): List<KSAnnotated> {
        val zkMetadataClasses = allKotlinFiles
            .flatMap(this::getAllInstancesOfZKTransactionMetadataCommandData)
            .toList()

        if (zkMetadataClasses.isNotEmpty()) {
            createMetaInfServicesFile(zkMetadataClasses)
                .appendText(
                    zkMetadataClasses.joinToString("\n") { "${it.qualifiedName?.asString()}\n" }
                )
        }

        return emptyList()
    }

    private fun getAllInstancesOfZKTransactionMetadataCommandData(ksFile: KSFile): List<KSClassDeclaration> {
        val visitor = ZKTransactionMetadataVisitor()
        visitor.visitFile(ksFile, Unit)
        return visitor.zkTransactionMetadataClasses
    }

    @Suppress("SpreadOperator")
    private fun createMetaInfServicesFile(correctlyAnnotatedTransactions: List<KSClassDeclaration>) =
        environment.codeGenerator.createNewFile(
            Dependencies(
                false,
                *correctlyAnnotatedTransactions.mapNotNull { it.containingFile }.toList().toTypedArray()
            ),
            "META-INF/services",
            ZKTransactionMetadataCommandData::class.packageName,
            ZKTransactionMetadataCommandData::class.simpleName!!
        )
}

private fun OutputStream.appendText(text: String) = use {
    write(text.toByteArray(StandardCharsets.UTF_8))
}
