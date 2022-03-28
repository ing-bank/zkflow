package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

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
        val sortedList = mutableListOf<KSClassDeclaration>()
        val successorMap = predecessorMap.filter { (_, predecessor) ->
            predecessor != null
        }.map { (current, predecessor) ->
            predecessor to current
        }.toMap()
        var currentElement = predecessorMap.filterValues { it == null }.keys.single() // first element
        var successor = successorMap[currentElement]
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
        val groupedStateDeclarations = stateDeclarations.groupBy {
            it.superTypes.single { superType ->
                familyNames.any { familyName ->
                    familyName == superType.resolve().declaration.qualifiedName?.asString()
                }
            }.toString()
        }
        return groupedStateDeclarations.map { (stateName, declarationsOfThisFamily) ->
            val predecessorMap = declarationsOfThisFamily.associateWith { it.getPredecessor(declarationsOfThisFamily) }
            val sortedDeclarations =
                sortStateDeclarations(predecessorMap) // sort declarations
            stateName to sortedDeclarations
        }.toMap()
    }
}
