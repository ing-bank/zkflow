package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Assumes that the version groups are correctly sorted by [StateVersionSorting].
 */
object VersionedStateIdGenerator {
    @JvmName("generateIds1")
    fun generateIds(sortedFamiliesMap: Map<KSClassDeclaration, List<KSClassDeclaration>>): Map<KSClassDeclaration, Int> =
        sortedFamiliesMap
            .entries
            .fold(emptyMap()) { acc, (stateFamily, declarationsOfThisFamily) ->
                val localIdMap = declarationsOfThisFamily
                    .mapIndexed { index, ksClassDeclaration ->
                        ksClassDeclaration to generateId(stateFamily, index)
                    }
                    .toMap()
                acc + localIdMap
            }

    private fun generateId(stateFamily: KSClassDeclaration, versionNumber: Int): Int {
        return (stateFamily.qualifiedName?.asString() + "$versionNumber").hashCode()
    }
}
