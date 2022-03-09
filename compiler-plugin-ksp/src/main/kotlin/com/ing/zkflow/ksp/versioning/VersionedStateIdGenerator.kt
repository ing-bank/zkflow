package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.symbol.KSClassDeclaration

object VersionedStateIdGenerator {
    fun generateIds(sortedFamiliesMap: Map<String, List<KSClassDeclaration>>): Map<KSClassDeclaration, Int> {
        var stateIdMap = emptyMap<KSClassDeclaration, Int>()
        sortedFamiliesMap.forEach {
            stateFamily, declarationsOfThisFamily ->
            val versionNumbers = IntRange(0, declarationsOfThisFamily.size - 1).toList()
            val stateIds = versionNumbers.map { generateId(stateFamily, it) }
            val localIdMap = declarationsOfThisFamily.zip(stateIds).toMap()
            stateIdMap += localIdMap
        }
        return stateIdMap
    }

    private fun generateId(stateFamily: String, versionNumber: Int): Int {
        return (stateFamily + "$versionNumber").hashCode()
    }
}
