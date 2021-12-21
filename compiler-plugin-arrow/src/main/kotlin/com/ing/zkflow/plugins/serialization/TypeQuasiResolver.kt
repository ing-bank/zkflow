package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.SerdeLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtVisitor

data class TypeQuasiResolver(val imports: KtImportList?) {
    init {
        imports?.accept(CollectWildcardImports, Unit)?.let { wildcardImports ->
            SerdeLogger.log(wildcardImports.joinToString(separator = "\n"))
            require(wildcardImports.isEmpty()) {
                "Wildcard imports are not permitted. Please specialize: ${wildcardImports.joinToString(separator = "\n")}"
            }
        }
    }

    fun resolve(simpleName: String): FqName? {
        return imports?.accept(TypeResolverVisitor, simpleName)
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
