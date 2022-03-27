package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.symbol.KSClassDeclaration

object VersionedStateIdGenerator {
    fun generateIds(sortedFamiliesMap: Map<String, List<KSClassDeclaration>>): Map<KSClassDeclaration, Int> =
        sortedFamiliesMap
            .entries
            .fold(emptyMap()) { acc, (stateFamily, declarationsOfThisFamily) ->
                val localIdMap = List(declarationsOfThisFamily.size) { it }
                    .map { version -> generateId(stateFamily, version) }
                    .zip(declarationsOfThisFamily) { id, declaration -> declaration to id }
                    .toMap()
                acc + localIdMap
            }

    private fun generateId(stateFamily: String, versionNumber: Int): Int {
        return (stateFamily + "$versionNumber").hashCode()
    }
}
