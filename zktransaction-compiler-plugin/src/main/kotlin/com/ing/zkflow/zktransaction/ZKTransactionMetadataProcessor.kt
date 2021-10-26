package com.ing.zkflow.zktransaction

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zkflow.common.contracts.ZKTransactionMetadata
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.zktransaction.Result.Companion.error
import com.ing.zkflow.zktransaction.Result.Companion.ok
import net.corda.core.internal.packageName
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class ZKTransactionMetadataProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val analysisResults: List<Result<KSClassDeclaration, KSClassDeclaration>> = resolver
            .getSymbolsWithAnnotation(ZKTransactionMetadata::class.qualifiedName!!)
            .mapNotNull { it as? KSClassDeclaration }
            .map { if (it.implementsZKTransactionMetadataCommandData()) ok(it) else error(it) }
            .toList()

        val classesNotImplementingZkTransactionMetadataCommandData = analysisResults.mapNotNull { it.error }
        if (classesNotImplementingZkTransactionMetadataCommandData.isNotEmpty()) {
            val classes = classesNotImplementingZkTransactionMetadataCommandData.joinToString {
                it.qualifiedName!!.asString()
            }
            throw IllegalStateException(
                "The following classes are annotated with @${ZKTransactionMetadata::class.simpleName}," +
                    " but don't implement ${ZKTransactionMetadataCommandData::class.qualifiedName}: $classes"
            )
        }

        val correctlyAnnotatedTransactionClasses = analysisResults.mapNotNull { it.ok }
        if (correctlyAnnotatedTransactionClasses.isNotEmpty()) {
            createMetaInfServicesFile(correctlyAnnotatedTransactionClasses)
                .appendText(
                    correctlyAnnotatedTransactionClasses
                        .joinToString("\n") { "${it.qualifiedName?.asString()}\n" }
                )
        }

        return emptyList()
    }

    @Suppress("SpreadOperator")
    private fun createMetaInfServicesFile(correctlyAnnotatedTransactions: List<KSClassDeclaration>) =
        environment.codeGenerator.createNewFile(
            Dependencies(
                true,
                *correctlyAnnotatedTransactions.mapNotNull { it.containingFile }.toList().toTypedArray()
            ),
            "META-INF/services",
            ZKTransactionMetadataCommandData::class.packageName,
            ZKTransactionMetadataCommandData::class.simpleName!!
        )
}

private data class Result<out R, out E>(
    val ok: R?,
    val error: E?,
) {
    companion object {
        fun <R> ok(value: R) = Result(value, null)
        fun <E> error(value: E) = Result(null, value)
    }
}

private fun OutputStream.appendText(text: String) = use {
    write(text.toByteArray(StandardCharsets.UTF_8))
}

private fun KSClassDeclaration.implementsZKTransactionMetadataCommandData(): Boolean {
    return this.getAllSuperTypes()
        .any {
            ZKTransactionMetadataCommandData::class.qualifiedName == it.declaration.qualifiedName?.asString()
        }
}
