package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.ing.zkflow.ksp.implementsInterfaceDirectly

object StateVersionSorting {
    fun sortByConstructors(
        logger: KSPLogger,
        markerGroups: Map<KSClassDeclaration, List<KSClassDeclaration>>
    ): Map<KSClassDeclaration, List<KSClassDeclaration>> {
        return markerGroups.map { (groupInterface, members) ->
            sortByConstructors(logger, groupInterface, members)
        }.toMap()
    }

    private fun sortByConstructors(
        logger: KSPLogger,
        groupInterface: KSClassDeclaration,
        members: List<KSClassDeclaration>
    ): Pair<KSClassDeclaration, List<KSClassDeclaration>> {
        val predecessorMap = members.associateWith { it.getPredecessor(groupInterface) }

        ensureOnlyOneWithoutPredecessor(predecessorMap)
        ensureNoCircularUpgradeRoutes(predecessorMap)

        val sortedDeclarations = sortDeclarations(predecessorMap)
        logger.info("Sorted version group $groupInterface: ${sortedDeclarations.joinToString(", ")}")
        return groupInterface to sortedDeclarations
    }

    private fun ensureNoCircularUpgradeRoutes(predecessorMap: Map<KSClassDeclaration, KSClassDeclaration?>) {
        val predecessors = predecessorMap.values.mapNotNull { it }
        val duplicates = predecessors.groupBy({ it }) { 1 }.mapValues { it.value.sum() }.filterValues { it > 1 }
        require(duplicates.isEmpty()) {
            "Each version group members should be constructable from a unique previous version. " +
                "Found multiple that are upgradable from the following previous versions: ${duplicates.keys.joinToString(", ")}"
        }
    }

    private fun ensureOnlyOneWithoutPredecessor(predecessorMap: Map<KSClassDeclaration, KSClassDeclaration?>) {
        val membersWithoutPredecessor = predecessorMap.filter { it.value == null }
        require(membersWithoutPredecessor.size == 1) {
            "In version groups, there must be exactly one version group member without a constructor for a previous version. " +
                "Found ${membersWithoutPredecessor.size} without constructor: ${
                membersWithoutPredecessor.keys.joinToString(", ")
                }."
        }
    }

    private fun KSClassDeclaration.getPredecessor(
        groupInterface: KSClassDeclaration,
    ): KSClassDeclaration? {
        val constructors = declarations.filterIsInstance<KSFunctionDeclaration>()
            .filter { it.isConstructor() }

        val upgradeConstructors = constructors.filter {
            it.parameters.size == 1 &&
                it.parameters.single().type.resolve().declaration is KSClassDeclaration &&
                (it.parameters.single().type.resolve().declaration as KSClassDeclaration).implementsInterfaceDirectly(groupInterface)
        }
        val upgradeConstructorCount = upgradeConstructors.count()
        if (upgradeConstructorCount == 0) return null

        val upgradeConstructor =
            upgradeConstructors.singleOrNull()
                ?: error("$this should have exactly one upgrade constructor. Found $upgradeConstructorCount.")

        return upgradeConstructor.parameters.single().type.resolve().declaration as KSClassDeclaration
    }

    /**
     * Sort the declarations according to the order induced by the `predecessorMap` . Time complexity: O(n).
     */
    private fun sortDeclarations(
        predecessorMap: Map<KSClassDeclaration, KSClassDeclaration?>
    ): List<KSClassDeclaration> {
        val successorMap = predecessorMap.filter { (_, predecessor) ->
            predecessor != null
        }.map { (current, predecessor) ->
            predecessor to current
        }.toMap()
        var currentElement = predecessorMap
            .filterValues { it == null }
            .keys
            .singleOrNull() ?: error("Could not find first of: $predecessorMap")
        var successor = successorMap[currentElement]
        val sortedList = mutableListOf<KSClassDeclaration>()
        while (successor != null) {
            sortedList.add(currentElement)
            currentElement = successor
            successor = successorMap[currentElement]
        }
        sortedList.add(currentElement) // the last element doesn't have a successor
        return sortedList
    }
}
