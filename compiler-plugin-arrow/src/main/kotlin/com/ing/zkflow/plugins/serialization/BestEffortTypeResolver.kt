package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.SerdeIndex
import com.ing.zkflow.SerdeLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtVisitor

class BestEffortTypeResolver(ktFile: KtFile) {
    private val imports: KtImportList? = ktFile.importList

    init {
        imports?.accept(CollectWildcardImports, Unit)?.let { wildcardImports ->
            SerdeLogger.log(wildcardImports.joinToString(separator = "\n"))
            require(wildcardImports.isEmpty()) {
                "Wildcard imports are not permitted. Please specialize: ${wildcardImports.joinToString(separator = "\n")}"
            }
        }
    }

    fun resolve(simpleName: String): BestEffortResolvedType {
        imports?.accept(TypeResolverVisitor, simpleName)?.let {
            try {
                // Try if the class has been imported from `compiled` imports.
                val kclass = Class.forName(it.toString()).kotlin
                BestEffortResolvedType.FullyResolved(kclass)
            } catch (e: ClassNotFoundException) {
                // Class has been imported from the userspace.
                null
            }
        }?.let { return it }

        SerdeIndex[simpleName]?.let {
            return BestEffortResolvedType.FullyQualified(it.fqName, it.annotations)
        }

        return BestEffortResolvedType.AsIs(simpleName)
    }
}

object CollectWildcardImports : KtVisitor<List<String>, Unit>() {
    override fun visitImportList(importList: KtImportList, data: Unit): List<String> {
        return importList
            .imports
            .fold(mutableListOf()) { acc, importDirective ->
                if (importDirective.isAllUnder) {
                    acc += importDirective.text
                }
                acc
            }
    }
}

object TypeResolverVisitor : KtVisitor<FqName?, String>() {
    override fun visitImportList(importList: KtImportList, data: String): FqName? {
        return importList.imports.singleOrNull {
            it.importedFqName?.pathSegments()?.last()?.asString() == data
        }?.importPath?.fqName
    }
}
