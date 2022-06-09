package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.SerdeIndex
import com.ing.zkflow.SerdeLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtVisitor

class BestEffortTypeResolver(ktFile: KtFile) {
    private val packageFqName = ktFile.packageFqName
    private val imports: KtImportList? = ktFile.importList

    init {
        imports?.accept(CollectWildcardImports, Unit)?.let { wildcardImports ->
            require(wildcardImports.isEmpty()) {
                "Wildcard imports are not permitted. Please specialize: ${wildcardImports.joinToString(separator = "\n")}"
            }
        }
    }

    /**
     * Attempt to resolve class name using the following strategies.
     * Each consequent attempt delivers less informative results.
     * 1. imported class is a compiled class in some module,
     *
     * If class is not imported, it only makes sense that class with `simpleName` exists in the same package
     * possibly as a subclass of another class, e.g, com.company.Base.Companion.Child,
     * otherwise the Kotlin compiler would have not progressed to plugin invocations,
     * That is, its `className` is `packageFqName` + {possible inclusion} + `simpleName`.
     *
     * 2. class with `simpleName` under `packageFqName` is a compiled class in this module,
     * 3. class with `simpleName` under `packageFqName` is present in this module but has not been compiled yet, and might have been indexed,
     * 4. class with `simpleName` under `packageFqName` is imported from a different module (thus not indexed), but is not compiled yet
     * 5. if none succeeds, return `AsIs(simpleName)` with no additional information.
     */
    fun resolve(simpleName: String): BestEffortResolvedType {
        val import = imports?.accept(TypeResolverVisitor, simpleName)
        SerdeLogger.log("Imports:\n${imports?.text}\nSuitable import: $import")

        // Case: imported class is a compiled class in some module.
        import?.let { fqName ->
            try {
                val kClass = Class.forName(fqName.toString()).kotlin
                return BestEffortResolvedType.FullyResolved(kClass)
            } catch (_: ClassNotFoundException) { }
        }

        // Case: class with `className` is a compiled class in this package.
        try {
            val kClass = Class.forName("$packageFqName.$simpleName").kotlin
            return BestEffortResolvedType.FullyResolved(kClass)
        } catch (_: ClassNotFoundException) { }

        // Case: class is present in this module but has not been compiled yet,
        //       and thus might have been indexed.
        SerdeIndex.get(simpleName, "$packageFqName")?.let {
            return BestEffortResolvedType.FullyQualified(it.fqName, it.annotations)
        }

        // Case: class is imported from a different module (thus not indexed), but is not compiled yet.
        import?.let { fqName ->
            return BestEffortResolvedType.FullyQualified(fqName, listOf())
        }

        // Else:
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
