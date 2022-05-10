package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.ing.zkflow.common.versioning.Versioned

object StateVersionSorting {

    private fun KSClassDeclaration.getPredecessor(declarationsOfThisFamily: List<KSClassDeclaration>): KSClassDeclaration? {
        val constructorParameterTypes = declarations.filterIsInstance<KSFunctionDeclaration>()
            .filter { it.isConstructor() }
            .flatMap { _constructor -> _constructor.parameters.map { it.type.toString() } }
        val declarationNamesSet = declarationsOfThisFamily.map { "$it" }.toSet()
        val referredParameterType = constructorParameterTypes.filter { typeName ->
            typeName != "$this" && typeName in declarationNamesSet
        }.singleOrNull()
        return declarationsOfThisFamily.singleOrNull { "$it" == referredParameterType }
    }

    /**
     * Sort the declarations according to the order induced by the `predecessorMap` . Time complexity: O(n).
     */
    private fun sortStateDeclarations(
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

    /**
     *  Sort the class declarations based on the implicit version order defined by their constructors that upgrade
     *  from previous versions.
     */
    public fun buildSortedMap(
        familyNames: Set<String>,
        stateDeclarations: List<KSClassDeclaration>
    ): Map<String, List<KSClassDeclaration>> {
        // Group by available family names, but also
        // - keep track of orphaned states,
        // - require one family per stateDeclaration.
        // Quadratic complexity.
        val groupedStateDeclarations = stateDeclarations.groupBy { stateDeclaration ->
            // Select a family for this `stateDeclaration`.

            val families = stateDeclaration.superTypes.mapNotNull { superType ->
                val superTypeName = superType.resolve().declaration.qualifiedName?.asString()

                familyNames.singleOrNull { superTypeName == it }
            }.toList()

            val family = when (families.size) {
                0 -> error(
                    """
                        ${stateDeclaration.qualifiedName?.asString()} is expected to (transitively) implement `${Versioned::class.simpleName}` interface.
                        Found options are ${familyNames.joinToString(separator = ", ") {"`$it`"}}
                    """.trimIndent()
                )
                1 -> families.single()
                else -> error(
                    """
                        ${stateDeclaration.qualifiedName?.asString()} (transitively) implements `${Versioned::class.simpleName}` interface several times
                        via ${families.joinToString(separator = ", ") {"`$it`"}}
                    """.trimIndent()
                )
            }

            family
        }

        return groupedStateDeclarations.map { (stateName, declarationsOfThisFamily) ->
            val predecessorMap = declarationsOfThisFamily.associateWith { it.getPredecessor(declarationsOfThisFamily) }
            val sortedDeclarations = sortStateDeclarations(predecessorMap) // sort declarations
            stateName to sortedDeclarations
        }.toMap()
    }
}
