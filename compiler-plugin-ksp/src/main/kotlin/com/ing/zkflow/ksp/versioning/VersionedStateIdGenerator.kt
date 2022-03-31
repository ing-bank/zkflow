package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.symbol.KSClassDeclaration

object VersionedStateIdGenerator {
    fun generateIds(sortedFamiliesMap: Map<String, List<KSClassDeclaration>>): Map<KSClassDeclaration, Int> =
        sortedFamiliesMap
            .entries
            .fold(emptyMap()) { acc, (stateFamily, declarationsOfThisFamily) ->
                val localIdMap = declarationsOfThisFamily.mapIndexed { index, ksClassDeclaration ->
                    ksClassDeclaration to generateId(stateFamily, index)
                }.toMap()
                acc + localIdMap
            }

    private fun generateId(stateFamily: String, versionNumber: Int): Int {
        return (stateFamily + "$versionNumber").hashCode()
    }
}
