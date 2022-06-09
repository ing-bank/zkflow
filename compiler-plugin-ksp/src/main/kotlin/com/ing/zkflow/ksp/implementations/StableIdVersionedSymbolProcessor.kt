package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.contracts.ZKUpgradeCommandData
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistryProvider
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.ksp.MetaInfServiceRegister
import com.ing.zkflow.ksp.upgrade.UpgradeCommandGenerator
import com.ing.zkflow.ksp.versioning.StateVersionSorting
import com.ing.zkflow.ksp.versioning.VersionedStateIdGenerator
import com.ing.zkflow.processors.SerializerRegistryProcessor
import com.ing.zkflow.processors.SerializerRegistryProcessor.GeneratedSerializer
import com.ing.zkflow.processors.SerializerRegistryProcessor.GeneratedSerializer.Companion.toImplementationClass
import com.ing.zkflow.util.merge
import kotlin.reflect.KClass

/**
 * [StableIdVersionedSymbolProcessor] achieves several goals
 * 1. validates that all states/commands are versioned and reports all unversioned items,
 * 2. groups and sorts versioned states/commands w.r.t. to markers implementing [Versioned],
 * 3. registers serializers for states/commands.
 * 4. generates `upgrade` commands to go from one version of a state/command to the next one.
 *
 * for (1) we need (described for [ZKContractState], same applies to [ZKCommandData]):
 * - [Item 1] collect all classes/objects implementing [ZKContractState]
 * - [Item 2] collect all classes/objects implementing _both_ [Versioned], [ZKContractState]
 * => unversioned states = [Item 2] - [Item 1]; if this difference is non-empty, report.
 *
 * for (2) we need (described for [ZKContractState], same applies to [ZKCommandData]):
 * - [Item 1] collect all interfaces extending [ZKContractState]
 * - [Item 2] collect all classes/objects implementing _both_ [Versioned], [ZKContractState]
 * => split by Versioned, order [ZKContractState]'s by constructor definitions.
 *
 * To summarize, for (1) and (2) we need to collect implementors of
 * - [Versioned]
 * - [ZKContractState], [ZKCommandData]
 * - [Versioned] _and_ [ZKContractState]; [Versioned] _and_ [ZKCommandData]
 *
 * This data is collected by [ImplementationsVisitor], next we can proceed to execution of the items (1) to (4) inclusive.
 */
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
        logger.info("${this::class.simpleName} New Files:\n${newFiles.joinToString("\n") { it.filePath }}")
        visitedFiles.addAll(newFiles)

        val implementations: Map<Set<KClass<*>>, List<ScopedDeclaration>> = newFiles
            .fold(emptyMap<ImplementationRequirement, List<ScopedDeclaration>>()) { acc, file ->
                val matches = implementationsVisitor.visitFile(file, null)
                acc.merge(matches)
            }
            .mapKeys { (key, _) -> key.superTypes }

        // Only consider non-abstract declarations
        val instances = implementations.mapValues { (_, value) ->
            value.filter { !it.declaration.isAbstract() }
        }
        checkForUnversionedStatesAndCommands(instances)

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
        val upgradeCommands: Map<GeneratedSerializer, Int> = listOf(sortedStateFamilies, sortedCommandFamilies)
            .flatMap { UpgradeCommandGenerator(codeGenerator).process(it) }
            .associateWith { it.className.canonicalName.hashCode() }

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
        internal val noVersioningRequiredCommandData = setOf(ZKUpgradeCommandData::class)

        private val implementationsVisitor = ImplementationsVisitor(
            listOf(
                ImplementationRequirement.isInterface(versioned),

                ImplementationRequirement.isClassOrObject(contractStates),
                ImplementationRequirement.isClassOrObject(versionedContractStates),

                ImplementationRequirement.isClassOrObject(commandData),
                ImplementationRequirement.isClassOrObject(versionedCommandData),
                ImplementationRequirement.isClassOrObject(noVersioningRequiredCommandData),
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
            additionalClasses: Map<GeneratedSerializer, Int> = emptyMap()
        ): ServiceLoaderRegistration {
            val sortedWithIds = VersionedStateIdGenerator
                .generateIds(this)
                .mapKeys { it.key.toImplementationClass() }
            return processor.process(sortedWithIds + additionalClasses)
        }

        private fun buildSortedMap(
            interfaceClass: KClass<out Any>,
            implementations: Map<Set<KClass<*>>, List<ScopedDeclaration>>,
            markers: Set<String>
        ): Map<String, List<KSClassDeclaration>> {

            return implementations[versioned + interfaceClass]?.let { impls ->
                val declarations = impls.map { it.declaration }
                try {
                    StateVersionSorting.buildSortedMap(markers, declarations)
                } catch (e: Error) {
                    error(
                        """
                            Cannot correctly sort versioned classes `${interfaceClass.qualifiedName}`"):
                            Declarations: ${declarations.joinToString(separator = ", ") { "${it.qualifiedName?.asString()}" }}
                            Markers: ${markers.joinToString(separator = ", ")}
                        """.trimIndent()
                    )
                }
            } ?: emptyMap()
        }

        internal fun checkForUnversionedStatesAndCommands(instances: Map<Set<KClass<*>>, List<HasQualifiedName>>) {
            val unversionedStates =
                filterUnversionedImplementations(instances, contractStates, versionedContractStates, emptySet())
            val unversionedCommands =
                filterUnversionedImplementations(instances, commandData, versionedCommandData, noVersioningRequiredCommandData)

            reportPossibleUnversionedException(unversionedStates, unversionedCommands)
        }

        /**
         *  Filter for the unversioned `implementations` of a particular interface set.
         *  @param implementations: The map that tells you for each set of interfaces, what implementations of this set were
         *  found.
         *  @param typeSet: The set of interfaces we are interested in that doesn't include the `Versioned` interface.
         *  @param versionedTypeSet: The set of interfaces we are interested in that does include the `Versioned` interface or is otherwise not required to be versioned.
         *  @param noVersionRequiredTypeSet: The set of interfaces we are interested in that are explicitly not required to be versioned.
         *  @return The difference between `versionedTypeSet` and `typeSet` in `implementations`.
         */
        private fun filterUnversionedImplementations(
            implementations: Map<Set<KClass<*>>, List<HasQualifiedName>>,
            typeSet: Set<KClass<*>>,
            versionedTypeSet: Set<KClass<*>>,
            noVersionRequiredTypeSet: Set<KClass<*>>
        ): Set<String> =
            implementations[typeSet].toQualifiedNameSet() - (implementations[versionedTypeSet].toQualifiedNameSet() + implementations[noVersionRequiredTypeSet].toQualifiedNameSet())

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
