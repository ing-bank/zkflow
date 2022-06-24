@file:Suppress("DEPRECATION") // We need to support AutomaticHashConstraint

package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.ksp.MetaInfServiceRegister
import com.ing.zkflow.ksp.findClassesOrObjectsWithAnnotation
import com.ing.zkflow.ksp.getAllImplementedInterfaces
import com.ing.zkflow.ksp.getAllSurrogates
import com.ing.zkflow.ksp.implementations.ImplementationRequirement
import com.ing.zkflow.ksp.implementations.ImplementationsVisitor
import com.ing.zkflow.ksp.implementations.ScopedDeclaration
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.ing.zkflow.ksp.implementations.getNewFiles
import com.ing.zkflow.ksp.implementsInterface
import com.ing.zkflow.ksp.implementsInterfaceDirectly
import com.ing.zkflow.ksp.upgrade.UpgradeCommandGenerator
import com.ing.zkflow.ksp.versioning.VersionFamilyGenerator
import com.ing.zkflow.ksp.versioning.VersionSorting
import com.ing.zkflow.ksp.versioning.VersionedCommandIdGenerator
import com.ing.zkflow.ksp.versioning.VersionedStateIdGenerator
import com.ing.zkflow.util.merge
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

/**
 * This processor generates serialization logic for all classes in the CorDapp that are annotated with either @ZKP or @ZKPSurrogate,
 * so that the fixed-length BFLSerializationScheme knows how to handle them.
 *
 * A strong assumption behind all logic in this processor is that for @ZKP or @ZKPSurrogate annotated classes, the properties of their
 * primary constructor are stable: i.e. they will not change ever again once they have been 'deployed'. They can change during development,
 * but never after deployment and usage in production. If they did, it would break deserialization of serialized classses.
 * If a change is required that would change the properties of the primary constructor, users should create a new version of that type.
 * This 'versioning' is left in the hands of the user for normal types. They can simply create new types when they need them.
 * ZKFLow will enforce that types never change after deployment. This ensures that when a user introduces a new version for a type that is
 * used in another user type, and they want to use it, they will also have to introduce a new type for that using type.
 *
 * Another strong assumption this processor makes is that all checks from [ZKPAnnotatedValidator] were executed and successful. This
 * processor does only limited validation itself.
 *
 * There is special treatment for implementors of [ContractState]. This is because [ContractState]s are the only top-level transaction
 * components in a Corda transaction that are reused across transactions. Since this happens often, ZKFLow offers a convencience feature to
 * easily use old states in new transactions: it generates upgrade commands from old to newer. In order to do that, the 'versioning' of
 * states is more formalized than for other types. They are required:
 * - to implement a so-called versioning 'marker' interface that identifies a version family/group.  Such a version group identifies a group
 *   of classes that are considered one type that can be upgraded to newer versions within that group. Such a marker interface
 *   is itself required to extend from [VersionedContractStateGroup]. Both these relations are required to be direct, i.e. direct supertypes, not through
 *   other parents. Example: `MyContractStateV1` and `MyContractStateV2` implement `interface VersionedMyContractState: VersionedContractStateGroup, ContractState`.
 * - to have additional secondary constructors that make clear an order within such a version group. Example: `MyContractStateV2` will have a constructor that
 *   that takes `MyContractStateV1` as its single parameter. When all classes withing a version group have these constructors, this processor
 *   can determine an order within that group.
 */
class ZKPAnnotatedProcessor(private val logger: KSPLogger, codeGenerator: CodeGenerator) : SymbolProcessor {
    private val visitedFiles: MutableSet<KSFile> = mutableSetOf()

    private val metaInfServiceRegister = MetaInfServiceRegister(codeGenerator)

    private val serializerProviderGenerator = SerializerProviderGenerator(codeGenerator)
    private val upgradeCommandGenerator = UpgradeCommandGenerator(codeGenerator)
    private val versionFamilyGenerator = VersionFamilyGenerator(codeGenerator)
    private val surrogateSerializerGenerator = SurrogateSerializerGenerator(codeGenerator)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val newFiles = getNewFiles(resolver)

        val zkpAnnotated = resolver.findClassesOrObjectsWithAnnotation(ZKP::class)
        val contractStateGroups = getSortedContractStateVersionGroups(newFiles, zkpAnnotated)
        val commands = zkpAnnotated.filter { it.implementsInterface(CommandData::class) }

        val commandsWithIds = VersionedCommandIdGenerator.generateIds(commands)
        val statesWithIds = VersionedStateIdGenerator.generateIds(contractStateGroups)
        val upgradeCommandsWithIds = upgradeCommandGenerator.generateUpgradeCommands(contractStateGroups.values)

        val surrogates = resolver.getAllSurrogates()

        surrogateSerializerGenerator.processZKPAnnotated(zkpAnnotated)
        surrogateSerializerGenerator.processZKPSurrogateAnnotated(surrogates)

        versionFamilyGenerator.generateFamilies(contractStateGroups).registerToServiceLoader()
        serializerProviderGenerator.generateProviders(statesWithIds + commandsWithIds + upgradeCommandsWithIds).registerToServiceLoader()

        metaInfServiceRegister.emit()

        return emptyList()
    }

    private fun getSortedContractStateVersionGroups(
        newFiles: Set<KSFile>,
        zkpAnnotated: Sequence<KSClassDeclaration>
    ): Map<KSClassDeclaration, List<KSClassDeclaration>> {
        val versionMarkerInterfaces: Sequence<KSClassDeclaration> = findVersionMarkerInterfaces(newFiles)
        val states = zkpAnnotated.filter { it.implementsInterface(ContractState::class) }
        val (unversioned, versionGroups) = groupByMarkerInterface(states, versionMarkerInterfaces)
        requireAllContractStatesVersioned(unversioned)
        return VersionSorting.sortByConstructors(logger, versionGroups)
    }

    /**
     * All marker interfaces must directly extend [VersionedContractStateGroup], not through any other interface.
     */
    private fun ensureMarkersDirectlyImplementVersioned(versionMarkerInterfaces: Sequence<KSClassDeclaration>) {
        versionMarkerInterfaces.forEach {
            require(it.implementsInterfaceDirectly(VersionedContractStateGroup::class)) {
                "$it is an interface that extends a version marker interface. This is not allowed. " + "Version marker interfaces should only ever be implemented by @ZKP or @ZKPSurrogate annotated classes or objects. " + "If it is meant to be a version marker itself it must implement ${VersionedContractStateGroup::class} directly. It does it indirectly. "
            }
        }
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

    private fun requireAllContractStatesVersioned(unversioned: List<KSClassDeclaration>) {
        if (unversioned.isNotEmpty()) {
            // Only ContractState is required to belong to a version group: it is required to be able to generate upgrade commands for them
            // This is not required for other types.
            val requiredToBeVersioned = unversioned.filter { it ->
                it.getAllImplementedInterfaces().any {
                    it.qualifiedName?.asString() == ContractState::class.qualifiedName
                }
            }
            if (requiredToBeVersioned.isNotEmpty()) {
                throw UnversionedException(
                    "All ${ContractState::class.simpleName}s annotated with @ZKP or @ZKPSurrogate must implement a version marker interface. " +
                        "The following do not: ${requiredToBeVersioned.joinToString(", ") { it.qualifiedName?.asString().orEmpty() }}"
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
        versionMarkerInterfaces: Sequence<KSClassDeclaration>
    ): Pair<List<KSClassDeclaration>, Map<KSClassDeclaration, List<KSClassDeclaration>>> {
        val groups = zkpAnnotated.map { annotatedClass ->
            annotatedClass.getAllImplementedInterfaces().singleOrNull { it in versionMarkerInterfaces } to annotatedClass
        }.groupBy { it.first }.mapValues { it.value.map { it.second } }

        val unversioned = groups[null] ?: emptyList()
        val versionedGroups = groups.mapNotNull { if (it.key == null) null else it.key!! to it.value }.toMap()
        versionedGroups.forEach { ensureGroupMembersDirectlyImplementGroupMarker(it.key, it.value) }
        return unversioned to versionedGroups
    }

    /**
     * Rule: all members of a version group must *directly* implement the version group marker interface,
     * not through another layer of indirection.
     */
    private fun ensureGroupMembersDirectlyImplementGroupMarker(group: KSClassDeclaration, members: List<KSClassDeclaration>) {
        members.forEach {
            require(it.implementsInterfaceDirectly(group)) {
                "$it must implement version group $group directly, but does it indirectly"
            }
        }
    }

    private fun findVersionMarkerInterfaces(newFiles: Set<KSFile>): Sequence<KSClassDeclaration> {
        return findImplementors(newFiles, VersionedContractStateGroup::class).filter { it.classKind == ClassKind.INTERFACE }
            .also { ensureMarkersDirectlyImplementVersioned(it) }
    }

    private fun findImplementors(newFiles: Set<KSFile>, implementedInterface: KClass<*>): Sequence<KSClassDeclaration> {
        val versionedRequirement = ImplementationRequirement(superTypes = setOf(implementedInterface))
        val implementationsVisitor = ImplementationsVisitor(implementationRequirements = listOf(versionedRequirement))
        val implementors: List<KSClassDeclaration> =
            newFiles.fold(emptyMap<ImplementationRequirement, List<ScopedDeclaration>>()) { acc, file ->
                val matches = implementationsVisitor.visitFile(file, null)
                acc.merge(matches)
            }.getOrDefault(versionedRequirement, emptyList()).map { it.declaration }
        return implementors.asSequence()
    }

    private fun getNewFiles(resolver: Resolver): Set<KSFile> {
        val newFiles = resolver.getNewFiles(visitedFiles)
        logger.info("${this::class.simpleName} New Files:\n${newFiles.joinToString("\n") { it.filePath }}")
        visitedFiles.addAll(newFiles)
        return newFiles
    }

    private class UnversionedException(message: String) : Exception(message)
}
