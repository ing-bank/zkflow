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

class StableIdVersionedSymbolProcessor(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) :
    SymbolProcessor {
    private val visitedFiles: MutableSet<KSFile> = mutableSetOf()
    private val metaInfServiceRegister = MetaInfServiceRegister(codeGenerator)

    private val stateRegistryProcessor =
        SerializerRegistryProcessor(
            ZKContractState::class,
            ContractStateSerializerRegistryProvider::class,
            codeGenerator
        )
    private val commandRegistryProcessor =
        SerializerRegistryProcessor(
            ZKCommandData::class,
            CommandDataSerializerRegistryProvider::class,
            codeGenerator
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
        val markers: Set<String> = extractMarkers(implementations)
        if (markers.isEmpty()) {
            // Early exit, there are _NO_ marker interfaces.
            return emptyList()
        }

        val sortedStateFamilies = buildSortedMap(ZKContractState::class, implementations, markers)
        val sortedCommandFamilies = buildSortedMap(ZKCommandData::class, implementations, markers)

        // Generate upgrade commands for states and commands
        val upgradeCommands: Map<ClassName, Int> = listOf(sortedStateFamilies, sortedCommandFamilies)
            .flatMap { UpgradeCommandGenerator(codeGenerator).process(it) }
            .associateWith { it.canonicalName.hashCode() }

        sortedStateFamilies.generateIdsAndProcessWith(stateRegistryProcessor)
            .registerToServiceLoader()
        sortedCommandFamilies.generateIdsAndProcessWith(commandRegistryProcessor, additionalClasses = upgradeCommands)
            .registerToServiceLoader()

        metaInfServiceRegister.emit()

        return emptyList()
    }

    private fun ServiceLoaderRegistration.registerToServiceLoader() {
        if (implementations.isNotEmpty()) {
            @Suppress("SpreadOperator")
            metaInfServiceRegister.addImplementation(
                providerClass,
                *implementations.toTypedArray()
            )
        }
    }

    companion object {
        internal val versioned = setOf(Versioned::class)
        internal val versionedContractStates = setOf(Versioned::class, ZKContractState::class)
        internal val versionedCommandData = setOf(Versioned::class, ZKCommandData::class)
        internal val contractStates = setOf(ZKContractState::class)
        internal val commandData = setOf(ZKCommandData::class)

        private val implementationsVisitor = ImplementationsVisitor(
            listOf(
                versioned, versionedCommandData, versionedContractStates,
                setOf(ZKContractState::class), setOf(ZKCommandData::class)
            )
        )

        internal class UnversionedException(message: String) : Exception(message)

        internal fun extractMarkers(implementations: Map<Set<KClass<*>>, List<HasQualifiedName>>): Set<String> {
            val versionedMarkers = implementations[versioned].toQualifiedNameSet()
            val versionedStateMarkers = implementations[versionedContractStates].toQualifiedNameSet()
            val versionedCommandMarkers = implementations[versionedCommandData].toQualifiedNameSet()

            return versionedMarkers - versionedStateMarkers - versionedCommandMarkers
        }

        private fun List<HasQualifiedName>?.toQualifiedNameSet(): Set<String> =
            this?.map { it.qualifiedName }?.toSet() ?: emptySet()

        private fun Map<String, List<KSClassDeclaration>>.generateIdsAndProcessWith(
            processor: SerializerRegistryProcessor<out Any>,
            additionalClasses: Map<ClassName, Int> = emptyMap()
        ): ServiceLoaderRegistration {
            val sortedWithIds = VersionedStateIdGenerator
                .generateIds(this)
                .mapKeys { (classDecl, _) -> classDecl.toClassName() }
            return processor.process(sortedWithIds + additionalClasses)
        }

        private fun buildSortedMap(
            interfaceClass: KClass<out Any>,
            implementations: Map<Set<KClass<*>>, List<ScopedDeclaration>>,
            markers: Set<String>
        ) = implementations[versioned + interfaceClass]?.let { impls ->
            StateVersionSorting.buildSortedMap(markers, impls.map { it.declaration })
        } ?: emptyMap()

        internal fun checkForUnversionedStatesAndCommands(implementations: Map<Set<KClass<*>>, List<HasQualifiedName>>) {
            val unversionedStates =
                filterUnversionedImplementations(
                    implementations,
                    contractStates,
                    versionedContractStates
                )
            val unversionedCommands =
                filterUnversionedImplementations(implementations, commandData, versionedCommandData)
            reportPossibleUnversionedException(unversionedStates, unversionedCommands)
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
            implementations: Map<Set<KClass<*>>, List<HasQualifiedName>>,
            typeSet: Set<KClass<*>>,
            versionedTypeSet: Set<KClass<*>>
        ): Set<String> =
            implementations[typeSet].toQualifiedNameSet() - implementations[versionedTypeSet].toQualifiedNameSet()

        internal fun reportPossibleUnversionedException(
            unversionedStateDeclarations: Set<String>,
            unversionedCommandDeclarations: Set<String>
        ) {
            val errorBuilder = StringBuilder()
            if (unversionedStateDeclarations.isNotEmpty()) {
                unversionedStateDeclarations.joinTo(
                    errorBuilder,
                    prefix = "ERROR: Unversioned ${ZKContractState::class.simpleName}'s found: [\n",
                    separator = "",
                    postfix = "].\n"
                ) { "    $it,\n" }
            }
            if (unversionedCommandDeclarations.isNotEmpty()) {
                unversionedCommandDeclarations.joinTo(
                    errorBuilder,
                    prefix = "ERROR: Unversioned ${ZKCommandData::class.simpleName}'s found: [\n",
                    separator = "",
                    postfix = "].\n"
                ) { "    $it,\n" }
            }
            if (errorBuilder.isNotEmpty()) {
                errorBuilder.append("Please ensure every ${ZKContractState::class.simpleName} and ${ZKCommandData::class.simpleName} implements the ${Versioned::class.simpleName} interface.")
                throw UnversionedException(errorBuilder.toString())
            }
        }
    }
}
