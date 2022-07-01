package com.ing.zkflow.zinc.poet.generate.structure

import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.indent

/**
 * Compare [savedStructure] to [newStructure] and return all backwards incompatible changes.
 */
fun compare(
    savedStructure: List<ZkpStructureType>,
    newStructure: List<ZkpStructureType>
): List<BackwardsIncompatibleChange> {
    require(savedStructure.all { it is ZkpStructureClass }) {
        "Only lists of classes are supported"
    }
    require(newStructure.all { it is ZkpStructureClass }) {
        "Only lists of classes are supported"
    }
    return compareClasses(
        savedStructure.filterIsInstance<ZkpStructureClass>(),
        newStructure.filterIsInstance<ZkpStructureClass>()
    )
}

private fun compareClasses(
    savedStructure: List<ZkpStructureClass>,
    newStructure: List<ZkpStructureClass>
): List<BackwardsIncompatibleChange> {
    // Create a list of related pairs
    val matchingPairs: List<Pair<ZkpStructureClass, ZkpStructureClass?>> = savedStructure
        .map {
            it to newStructure.findBySerializationIdOrSerialNameOf(it)
        }

    // 1. Check for removed classes
    val removedClasses: List<ZkpStructureClass> = matchingPairs
        .mapNotNull { (savedClass, newClass) -> if (newClass == null) savedClass else null }

    // 2. Check for modified classes
    val modifiedClasses: List<Pair<ZkpStructureClass, ZkpStructureClass>> = matchingPairs
        .mapNotNull { (savedClass, newClass) -> newClass?.let { Pair(savedClass, it) } }
        .filter { (savedClass, newClass) -> savedClass.isChanged(newClass) }

    return listOfNotNull(
        if (removedClasses.isNotEmpty()) RemovedClasses(removedClasses) else null,
        if (modifiedClasses.isNotEmpty()) ModifiedClasses(modifiedClasses) else null,
    )
}

private fun List<ZkpStructureClass>.findBySerializationIdOrSerialNameOf(
    searchTerm: ZkpStructureClass
) = singleOrNull { newType ->
    if (searchTerm.serializationId != null) {
        searchTerm.serializationId == newType.serializationId
    } else {
        searchTerm.serialName == newType.serialName
    }
}

sealed interface BackwardsIncompatibleChange

data class RemovedClasses(val classes: List<ZkpStructureClass>) : BackwardsIncompatibleChange {
    override fun toString(): String {
        val removedClasses = classes.joinToString("\n") {
            describeClass(it)
        }
        return """
            The following classes have been removed, please restore them:
                ${removedClasses.indent(16.spaces)}
        """.trimIndent()
    }
}

data class ModifiedClasses(
    val modifications: List<Pair<ZkpStructureClass, ZkpStructureClass>>
) : BackwardsIncompatibleChange {
    override fun toString(): String {
        val modifiedClasses = modifications.joinToString("\n") { (saved, new) ->
            val fieldChanges = findFieldChanges(saved.fields, new.fields).joinToString("\n") {
                "- ${it.describe()}"
            }
            """
                ${describeClass(saved)}
                    The following fields are modified, please undo these changes:
                    ${fieldChanges.indent(20.spaces)}
            """.trimIndent()
        }
        return """
            The following classes are modified, please restore them:
                ${modifiedClasses.indent(16.spaces)}
        """.trimIndent()
    }
}

private fun describeClass(it: ZkpStructureClass) =
    if (it.serializationId != null) {
        "- ${it.serialName} (id: ${it.serializationId})"
    } else {
        "- ${it.serialName}"
    }
