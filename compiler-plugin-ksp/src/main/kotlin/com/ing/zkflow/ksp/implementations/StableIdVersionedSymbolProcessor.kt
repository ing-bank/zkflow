package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistryProvider
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.ksp.MetaInfServiceRegister
import com.ing.zkflow.ksp.upgrade.UpgradeCommandGenerator
import com.ing.zkflow.ksp.versioning.StateVersionSorting
import com.ing.zkflow.ksp.versioning.VersionedStateIdGenerator
import com.ing.zkflow.processors.SerializerRegistryProcessor
import com.ing.zkflow.util.merge
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

class StableIdVersionedSymbolProcessor(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) : SymbolProcessor {
    private class UnversionedException(message: String) : Exception(message)

    private val visitedFiles: MutableSet<KSFile> = mutableSetOf()
    private val metaInfServiceRegister = MetaInfServiceRegister(codeGenerator)

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
        logger.info("New Files: ${newFiles.joinToString { it.filePath }}")
        visitedFiles.addAll(newFiles)

        val implementations: Map<Set<KClass<*>>, List<ScopedDeclaration>> = newFiles
            .fold(emptyMap()) { acc, file ->
                acc.merge(implementationsVisitor.visitFile(file, null))
            }

        checkForUnversionedStatesAndCommands(implementations)

        // It is true that
        // `implementations[versioned]` shall contain also `versionedContractStates` and `versionedCommandData`.
        // We shall filter out interfaces that do implement ONLY `versioned`.

        val markers: Set<String> = implementations[versioned]
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

        val upgradeCommands: Map<ClassName, Int> = generateUpgradeCommands(implementations, markers)

        listOf(
            Triple(
                contractState,
                SerializerRegistryProcessor(contractState, ContractStateSerializerRegistryProvider::class, codeGenerator),
                emptyMap()
            ),
            Triple(
                commandData,
                SerializerRegistryProcessor(commandData, CommandDataSerializerRegistryProvider::class, codeGenerator),
                upgradeCommands
            )
        ).forEach { (interfaceClass, processor, additionalClasses) ->
            implementations[versioned + interfaceClass]?.let { impls ->
                generateAndRegisterImplementations(markers, impls, processor, additionalClasses)
            }
        }

        metaInfServiceRegister.emit()

        return emptyList()
    }

    /**
     *  Filter for the unversioned `implementations` of a particular interface set.
     *  @param implementations: The map that tells you for each set of interfaces, what implementations of this set were
     *  found.
     *  @param typeSet: The set of interfaces we are interested in that doesn't include the `Versioned` interface.
     *  @param versionedTypeSet: The set of interfaces we are interested in that does include the `Versioned` interface.
     *  @return The difference between `versionedTypeSet` and `typeSet` in `implementations`.
     */
    private fun filterUnversionedImplementations(
        implementations: Map<Set<KClass<*>>, List<ScopedDeclaration>>,
        typeSet: Set<KClass<*>>,
        versionedTypeSet: Set<KClass<*>>
    ): List<ScopedDeclaration> = implementations[typeSet]?.filterNot {
        implementations[versionedTypeSet]?.contains(it)
            ?: true
    } ?: emptyList()

    private fun reportPossibleUnversionedException(unversionedStateDeclarations: List<ScopedDeclaration>, unversionedCommandDeclarations: List<ScopedDeclaration>) {
        var errorString = ""
        if (unversionedStateDeclarations.isNotEmpty()) {
            val unversionedStateDeclarationsString = unversionedStateDeclarations.joinToString(", ") { it.qualifiedName }
            errorString += "ERROR: Unversioned ${ZKContractState::class.simpleName}'s found: [ $unversionedStateDeclarationsString ] .\n"
        }

        if (unversionedCommandDeclarations.isNotEmpty()) {
            val unversionedCommandDeclarationsString = unversionedCommandDeclarations.joinToString(", ") { it.qualifiedName }
            errorString += "Unversioned ${ZKCommandData::class.simpleName}'s found: [ $unversionedCommandDeclarationsString ] .\n"
        }
        if (errorString.isNotEmpty()) {
            errorString += "Please ensure every ${ZKContractState::class.simpleName} and ${ZKCommandData::class.simpleName} implements the ${Versioned::class.simpleName} interface."
            throw UnversionedException(
                errorString
            )
        }
    }

    private fun generateAndRegisterImplementations(
        markers: Set<String>,
        impls: List<ScopedDeclaration>,
        processor: SerializerRegistryProcessor<out Any>,
        additionalClasses: Map<ClassName, Int>
    ) {
        val sorted: Map<String, List<KSClassDeclaration>> =
            StateVersionSorting.buildSortedMap(markers, impls.map { it.declaration })

        val sortedWithIds: Map<KSClassDeclaration, Int> = VersionedStateIdGenerator.generateIds(sorted)

        val registration =
            processor.process(sortedWithIds.mapKeys { (classDecl, _) -> classDecl.toClassName() } + additionalClasses)

        if (registration.implementations.isNotEmpty()) {
            @Suppress("SpreadOperator")
            metaInfServiceRegister.addImplementation(
                registration.providerClass,
                *registration.implementations.toTypedArray()
            )
        }
    }

    private fun checkForUnversionedStatesAndCommands(implementations: Map<Set<KClass<*>>, List<ScopedDeclaration>>) {
        val unversionedStates =
            filterUnversionedImplementations(implementations, setOf(contractState), versionedContractStates)
        val unversionedCommands =
            filterUnversionedImplementations(implementations, setOf(commandData), versionedCommandData)
        reportPossibleUnversionedException(unversionedStates, unversionedCommands)
    }

    private fun generateUpgradeCommands(
        implementations: Map<Set<KClass<*>>, List<ScopedDeclaration>>,
        markers: Set<String>
    ): Map<ClassName, Int> {
        val upgradeCommands: Map<ClassName, Int> = listOf(
            contractState,
            commandData,
        ).flatMap { interfaceClass ->
            implementations[versioned + interfaceClass]?.let { impls ->
                val sorted: Map<String, List<KSClassDeclaration>> =
                    StateVersionSorting.buildSortedMap(markers, impls.map { it.declaration })
                UpgradeCommandGenerator(codeGenerator).process(sorted)
            } ?: emptyList()
        }.associateWith {
            it.canonicalName.hashCode()
        }
        return upgradeCommands
    }
}
