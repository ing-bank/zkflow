package com.ing.zkflow.ksp.versioning

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zkflow.ksp.implementsInterface
import com.ing.zkflow.processors.SerializerRegistryProcessor
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState

/**
 * Assumes that the version groups are correctly sorted by [VersionSorting].
 */
object VersionedStateIdGenerator {
    fun generateIds(sortedStateMap: Map<KSClassDeclaration, List<KSClassDeclaration>>): Map<KSClassDeclaration, Int> =
        sortedStateMap
            .entries
            .fold(emptyMap()) { acc, (stateFamily, declarationsOfThisFamily) ->
                val localIdMap = declarationsOfThisFamily
                    .mapIndexed { index, ksClassDeclaration ->
                        require(ksClassDeclaration.implementsInterface(ContractState::class)) {
                            "$this can only generate ids for ${ContractState::class.simpleName}"
                        }
                        ksClassDeclaration to generateId(stateFamily, index)
                    }
                    .toMap()
                acc + localIdMap
            }

    private fun generateId(stateFamily: KSClassDeclaration, versionNumber: Int): Int {
        return (stateFamily.qualifiedName?.asString() + "$versionNumber").hashCode()
    }
}

object VersionedCommandIdGenerator {
    fun generateIds(commands: List<KSClassDeclaration>): Map<KSClassDeclaration, Int> =
        commands.fold(emptyMap()) { acc, declaration ->
            require(declaration.implementsInterface(CommandData::class)) {
                "$this can only generate ids for ${CommandData::class.simpleName}"
            }
            acc + (declaration to generateId(declaration))
        }

    fun generateId(serializer: SerializerRegistryProcessor.GeneratedSerializer): Int {
        val qualifiedName = serializer.className.canonicalName
        return generateId(qualifiedName)
    }

    private fun generateId(declaration: KSClassDeclaration): Int {
        val qualifiedName =
            declaration.qualifiedName?.asString() ?: error("Can't generate an id for $declaration, it has not qualified name.")
        return generateId(qualifiedName)
    }

    private fun generateId(qualifiedName: String): Int {
        return qualifiedName.hashCode()
    }
}
