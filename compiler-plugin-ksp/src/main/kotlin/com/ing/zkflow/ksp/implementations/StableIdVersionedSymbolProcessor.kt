package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistryProvider
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistryProvider
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.ksp.MetaInfServiceRegister
import com.ing.zkflow.ksp.getAllImplementedInterfaces
import com.ing.zkflow.ksp.implementsInterface
import com.ing.zkflow.ksp.implementsInterfaceDirectly
import com.ing.zkflow.ksp.upgrade.UpgradeCommandGenerator
import com.ing.zkflow.ksp.versioning.StateVersionSorting
import com.ing.zkflow.ksp.versioning.VersionedStateIdGenerator
import com.ing.zkflow.processors.SerializerRegistryProcessor
import com.ing.zkflow.processors.SerializerRegistryProcessor.GeneratedSerializer.Companion.toImplementationClass
import com.ing.zkflow.util.merge
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

/**
 * This processor validates all classes in the CorDapp that are annotated with either @ZKP or @ZKPSurrogate.
 * It also generates some serialization logic for them, so that the fixed-length BFLSerializationScheme knows how to handle them.
 *
 * These classes are required to support fixed-length serialization. To do that, they are required:
 * - to implement a so-called versioning 'marker' interface that identifies a version family/group.  Such a version group identifies a group
 *   of classes that are considered one type that can be upgraded to newer versions within that group. Such a marker interface
 *   is itself required to extend from [Versioned]. Both these relations are required to be direct, i.e. direct supertypes, not through
 *   other parents. Example: `MyContractStateV1` and `MyContractStateV2` implement `interface VersionedMyContractState: Versioned, ContractState`.
 *
 * - to have constructors that make clear an order within such a version group. Example: `MyContractStateV2` will have a constructor that
 *   that takes `MyContractStateV1` as its single parameter. When all classes withing a version group have these constructors, this processor
 *   can determine an order within that group.
 *
 * To achieve this, [StableIdVersionedSymbolProcessor] does the following:
 * 0. collect all versioned classes. These are classes annotated with either @ZKP or @ZKPSurrogate.
 *    Note that we require ContractState and CommandData to be annotated always. This is so that we at least guarantee top-level transaction
 *    components to be versioned. For lower level classes, we can't enforce this, since we don't know which ones will be part of a top-level
 *    ContractState or CommandData. Also, BFL serialization will fail anyway if a non-annotated class is used as a property type within a ContractState
 *    or CommandData.
 * 1. groups versioned classes within their version marker group, identified by a marker interface.
 * 2. validates that all classes annotated with @ZKP belong to a version marker group.
 * 3. sorts versioned classes within their version marker group according to their constructors. Will fail if there are classes within a
 *    group that are not part of the version chain.
 * 4. generates `upgrade` commands to go from one version of a ContractState to the next one.
 * 5. registers serializers in the registry for states/commands so that they can be serialized by a stable id instead
 *    of fqn by BFLSerializationScheme.
 */
class StableIdVersionedSymbolProcessor(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) :
    SymbolProcessor {
    private val visitedFiles: MutableSet<KSFile> = mutableSetOf()

    private val metaInfServiceRegister = MetaInfServiceRegister(codeGenerator)

    private val stateRegistryProcessor =
        SerializerRegistryProcessor(
            ContractState::class,
            ContractStateSerializerRegistryProvider::class,
            codeGenerator
        )
    private val commandRegistryProcessor =
        SerializerRegistryProcessor(
            CommandData::class,
            CommandDataSerializerRegistryProvider::class,
            codeGenerator
        )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val newFiles = getNewFiles(resolver)

        ensureZKPAnnotated(newFiles, ContractState::class)
        ensureZKPAnnotated(newFiles, CommandData::class)

        val zkpAnnotated: Sequence<KSClassDeclaration> = findClassesOrObjectsAnnotatedWithZKP(resolver)
        val versionMarkerInterfaces: List<KSClassDeclaration> = findVersionMarkerInterfaces(newFiles)

        // 2. groups and sorts versioned classes w.r.t. to markers implementing [Versioned],
        val (unversioned, markerGroups) = groupByMarkerInterface(zkpAnnotated, versionMarkerInterfaces)

        // 1. validates that all classes annotated with @ZKP are versioned and reports all unversioned items,
        requireImplementsMarker(unversioned)

        // 2. groups and sorts versioned classes w.r.t. to markers implementing [Versioned],
        val sortedMarkerGroups = StateVersionSorting.sortByConstructors(logger, markerGroups)

        val contractStateGroups = filterMembersImplementInterface(sortedMarkerGroups, ContractState::class)

        // 4. generates `upgrade` commands to go from one version of a state to the next one.
        // This is only done for ContractStates: they are the only top-level components re-used in new transactions
        val upgradeCommands: Map<SerializerRegistryProcessor.GeneratedSerializer, Int> =
            UpgradeCommandGenerator(codeGenerator)
                .processVersionGroups(contractStateGroups.values)
                .associateWith { it.className.canonicalName.hashCode() }

        // 3. registers serializers in the registry for states/commands so that they can be serialized with a short stable id
        // instead of a much longer fqn by BFLSerializationScheme.
        // SO ONLY STATES AND COMMANDS, since they are the only ones that are top-level from BFLSerializationScheme perspective.
        // For the rest, Arrow Meta will generate @Serializable(with) annotations and add inline serializers,
        // assuming the surrogate serializers were generated by KSP. This means they are automatically serializable without registration.
        contractStateGroups
            .generateIdsAndProcessWith(stateRegistryProcessor)
            .registerToServiceLoader()

        val commandFamilies = filterMembersImplementInterface(sortedMarkerGroups, CommandData::class)
        commandFamilies
            .generateIdsAndProcessWith(commandRegistryProcessor, additionalClasses = upgradeCommands)
            .registerToServiceLoader()

        metaInfServiceRegister.emit()

        return emptyList()
    }

    /**
     * All marker interfaces must directly extend [Versioned], not through any other interface.
     */
    private fun ensureMarkersDirectlyImplementVersioned(versionMarkerInterfaces: List<KSClassDeclaration>) {
        versionMarkerInterfaces.forEach {
            require(it.implementsInterfaceDirectly(Versioned::class)) {
                "$it is an interface that extends a version marker interface. This is not allowed. " +
                    "Version marker interfaces should only ever be implemented by @ZKP or @ZKPSurrogate annotated classes or objects. " +
                    "If it is meant to be a version marker itself it must implement ${Versioned::class} directly. It does it indirectly. "
            }
        }
    }

    private fun ensureZKPAnnotated(newFiles: Set<KSFile>, kClass: KClass<*>) {
        val implementors = findImplementors(newFiles, kClass)
            .filter { (it.classKind == ClassKind.CLASS || it.classKind == ClassKind.OBJECT) && !it.isAbstract() }

        implementors.forEach {
            require(it.isAnnotationPresent(ZKP::class) || it.isAnnotationPresent(ZKPSurrogate::class)) {
                "${it.qualifiedName?.asString()} is a ${kClass.simpleName} and must therefore be annotated with either @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName}"
            }
        }
    }

    /**
     * Returns a subset of the markergroups that implement a certain interface.
     *
     * If the group marker interface implements the interface, the whole group is returned.
     * If the group marker interface does NOT implement the interface, only the members of the group
     * that implement the interface are returned.
     */
    private fun filterMembersImplementInterface(
        sortedMarkerGroups: Map<KSClassDeclaration, List<KSClassDeclaration>>,
        interfaceClass: KClass<*>
    ): Map<KSClassDeclaration, List<KSClassDeclaration>> {
        val contractStateFamilies = sortedMarkerGroups.mapNotNull { (group, implementors) ->
            if (group.implementsInterface(interfaceClass)) {
                group to implementors
            } else if (implementors.any { it.implementsInterface(interfaceClass) }) {
                group to implementors.filter { it.implementsInterface(interfaceClass) }
            } else {
                null
            }
        }.toMap()
        return contractStateFamilies
    }

    private fun Map<KSClassDeclaration, List<KSClassDeclaration>>.generateIdsAndProcessWith(
        processor: SerializerRegistryProcessor<out Any>,
        additionalClasses: Map<SerializerRegistryProcessor.GeneratedSerializer, Int> = emptyMap()
    ): ServiceLoaderRegistration {
        val sortedWithIds = VersionedStateIdGenerator
            .generateIds(this)
            .mapKeys { it.key.toImplementationClass() }
        return processor.process(sortedWithIds + additionalClasses)
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

    private fun requireImplementsMarker(unversioned: List<KSClassDeclaration>) {
        if (unversioned.isNotEmpty()) {
            // TODO: now purposely only checks for ContractState and CommandData for backwards compat. Remove later to enforce versioned on any @ZKP* annotated.
            val requiredToBeVersioned = unversioned.filter { it ->
                it.getAllImplementedInterfaces().any {
                    it.qualifiedName?.asString() == ContractState::class.qualifiedName ||
                        it.qualifiedName?.asString() == CommandData::class.qualifiedName
                }
            }
            if (requiredToBeVersioned.isNotEmpty()) {
                throw UnversionedException(
                    "All types annotated with @ZKP or @ZKPSurrogate must implement a version marker interface. " +
                        "The following do not: ${requiredToBeVersioned.joinToString(", ")}"
                )
            }
        }
    }

    /**
     * Returns a pair where, the first component is the list of zkpAnnotated that implements no marker interface,
     * and the second component is are the zkpAnnotated grouped by versionMarkerInterfaces.
     */
    private fun groupByMarkerInterface(
        zkpAnnotated: Sequence<KSClassDeclaration>,
        versionMarkerInterfaces: List<KSClassDeclaration>
    ): Pair<List<KSClassDeclaration>, Map<KSClassDeclaration, List<KSClassDeclaration>>> {
        val groups = zkpAnnotated.map { annotatedClass ->
            annotatedClass.getAllImplementedInterfaces().singleOrNull { it in versionMarkerInterfaces } to annotatedClass
        }.groupBy { it.first }.mapValues { it.value.map { it.second } }

        val unversioned = groups[null] ?: emptyList()
        val versionedGroups = groups.mapNotNull { if (it.key == null) null else it.key!! to it.value }.toMap()
        versionedGroups.forEach { ensureGroupMembersDirectlyImplementGroupMarker(it.key, it.value) }
        return unversioned to versionedGroups
    }

    private fun ensureGroupMembersDirectlyImplementGroupMarker(group: KSClassDeclaration, members: List<KSClassDeclaration>) {
        members.forEach {
            require(it.implementsInterfaceDirectly(group)) {
                "$it must implement version group $group directly, but does it indirectly"
            }
        }
    }

    private fun findClassesOrObjectsAnnotatedWithZKP(resolver: Resolver): Sequence<KSClassDeclaration> {
        val zkpAnnotated = resolver.findClassesOrObjectsWithAnnotation("com.ing.zkflow.annotations.ZKP")
        val zkpSurrogateAnnotated = resolver.findClassesOrObjectsWithAnnotation("com.ing.zkflow.annotations.ZKPSurrogate")

        return zkpAnnotated + zkpSurrogateAnnotated
    }

    private fun Resolver.findClassesOrObjectsWithAnnotation(annotationName: String): Sequence<KSClassDeclaration> {
        val symbols = getSymbolsWithAnnotation(annotationName).filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind in listOf(ClassKind.CLASS, ClassKind.OBJECT) }
        logger.info("Annotated with $annotationName: ${symbols.joinToString(", ")}")
        return symbols
    }

    private fun findVersionMarkerInterfaces(newFiles: Set<KSFile>): List<KSClassDeclaration> {
        return findImplementors(newFiles, Versioned::class).filter { it.classKind == ClassKind.INTERFACE }
            .also { ensureMarkersDirectlyImplementVersioned(it) }
    }
//     val versionedRequirement = ImplementationRequirement.isInterface(superTypes = setOf(Versioned::class))
//     val implementationsVisitor = ImplementationsVisitor(implementationRequirements = listOf(versionedRequirement))
//     val versionMarkerInterfaces: List<KSClassDeclaration> =
//         newFiles.fold(emptyMap<ImplementationRequirement, List<ScopedDeclaration>>()) { acc, file ->
//             val matches = implementationsVisitor.visitFile(file, null)
//             acc.merge(matches)
//         }
//             .getOrDefault(versionedRequirement, emptyList())
//             .map { it.declaration }
//     logger.warn("Implementors of : $versionMarkerInterfaces")
//     return versionMarkerInterfaces
// }

    private fun findImplementors(newFiles: Set<KSFile>, implementedInterface: KClass<*>): List<KSClassDeclaration> {
        val versionedRequirement = ImplementationRequirement(superTypes = setOf(implementedInterface))
        val implementationsVisitor = ImplementationsVisitor(implementationRequirements = listOf(versionedRequirement))
        val implementors: List<KSClassDeclaration> =
            newFiles.fold(emptyMap<ImplementationRequirement, List<ScopedDeclaration>>()) { acc, file ->
                val matches = implementationsVisitor.visitFile(file, null)
                acc.merge(matches)
            }
                .getOrDefault(versionedRequirement, emptyList())
                .map { it.declaration }
        logger.info("Implementors of $implementedInterface: $implementors")
        return implementors
    }

    private fun getNewFiles(resolver: Resolver): Set<KSFile> {
        val newFiles = resolver.getNewFiles(visitedFiles)
        logger.info("${this::class.simpleName} New Files:\n${newFiles.joinToString("\n") { it.filePath }}")
        visitedFiles.addAll(newFiles)
        return newFiles
    }

    private class UnversionedException(message: String) : Exception(message)
}
