package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistryProvider
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.ksp.MetaInfServiceRegister
import com.ing.zkflow.ksp.versioning.StateVersionSorting
import com.ing.zkflow.ksp.versioning.VersionedStateIdGenerator
import com.ing.zkflow.processors.SerializerRegistryProcessor
import com.ing.zkflow.util.merge
import kotlin.reflect.KClass

class StableIdVersionedSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private class UnversionedException(message: String) : Exception(message)

    private val visitedFiles: MutableSet<KSFile> = mutableSetOf()
    private val metaInfServiceRegister = MetaInfServiceRegister(environment.codeGenerator)

    private val versioned = setOf(Versioned::class)

    private val contractState = ZKContractState::class
    private val versionedContractStates = setOf(Versioned::class, contractState)

    private val commandData = ZKCommandData::class
    private val versionedCommandData = setOf(Versioned::class, commandData)

    private val implementationsVisitor = ImplementationsVisitor(
        listOf(versioned, versionedCommandData, versionedContractStates, setOf(contractState), setOf(commandData))
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val newFiles = resolver.getNewFiles(visitedFiles)
        visitedFiles.addAll(newFiles)

        val implementations = newFiles
            .fold(emptyMap<Set<KClass<*>>, List<ScopedDeclaration>>()) { acc, file ->
                acc.merge(implementationsVisitor.visitFile(file, null))
            }

        checkForUnversionedStatesAndCommands(implementations)

        // It is true that
        // `implementations[versioned]` shall contain also `versionedContractStates` and `versionedCommandData`.
        // We shall filter out interfaces that do implement ONLY `versioned`.

        val markers = implementations[versioned]
            ?.filter { marker ->
                val fqName = marker.qualifiedName

                listOf(versionedContractStates, versionedCommandData).forEach { interfaces ->
                    implementations[interfaces]
                        ?.any { it.qualifiedName == fqName }
                        ?.let { return@filter !it }
                }

                true
            }
            ?.map { it.qualifiedName }
            ?.toSet()
            // Early exit, there are _NO_ marker interfaces.
            ?: return emptyList()

        listOf(
            Pair(
                contractState,
                SerializerRegistryProcessor(
                    contractState,
                    ContractStateSerializerRegistryProvider::class,
                    environment.codeGenerator
                )
            ),
            Pair(
                commandData,
                SerializerRegistryProcessor(
                    commandData,
                    CommandDataSerializerRegistryProvider::class,
                    environment.codeGenerator
                )
            )
        ).forEach { (interfaceClass, processor) ->
            implementations[versioned + interfaceClass]?.let { impls ->
                val sorted = StateVersionSorting.buildSortedMap(markers, impls.map { it.declaration })

                val sortedWithIds = VersionedStateIdGenerator.generateIds(sorted)

                val registration = processor.process(sortedWithIds)

                if (registration.implementations.isNotEmpty()) {
                    @Suppress("SpreadOperator")
                    metaInfServiceRegister.addImplementation(
                        registration.providerClass,
                        *registration.implementations.toTypedArray()
                    )
                }
            }
        }

        metaInfServiceRegister.emit()

        return emptyList()
    }

    private fun checkForUnversionedStatesAndCommands(implementations: Map<Set<KClass<*>>, List<ScopedDeclaration>>) {
        val unversionedStates = implementations[setOf(contractState)]?.filter {
            !(
                implementations[versionedContractStates]?.contains(it)
                    ?: true
                )
        } ?: emptyList()

        if (unversionedStates.isNotEmpty()) {
            val unversionedStatesString = unversionedStates.map { it.qualifiedName }.joinToString(", ")
            throw UnversionedException(
                "ERROR: Unversioned states found: $unversionedStatesString .\n " +
                    "Please ensure every contract state and command implements the Versioned interface."
            )
        }

        val unversionedCommands = implementations[setOf(commandData)]?.filter {
            !(
                implementations[versionedCommandData]?.contains(it)
                    ?: true
                )
        } ?: emptyList()

        if (unversionedCommands.isNotEmpty()) {
            val unversionedCommandsString = unversionedCommands.map { it.qualifiedName }.joinToString(", ")
            throw UnversionedException(
                "ERROR: Unversioned commands found: $unversionedCommandsString .\n " +
                    "Please ensure every contract state and command implements the Versioned interface."
            )
        }
    }
}
