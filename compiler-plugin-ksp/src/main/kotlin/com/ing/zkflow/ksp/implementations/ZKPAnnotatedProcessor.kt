@file:Suppress("DEPRECATION") // We need to support AutomaticHashConstraint

package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Visibility
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.ASCIIChar
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF16
import com.ing.zkflow.annotations.UTF32
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.UnicodeChar
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.annotations.corda.Algorithm
import com.ing.zkflow.annotations.corda.EcDSA_K1
import com.ing.zkflow.annotations.corda.EcDSA_R1
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.RSA
import com.ing.zkflow.annotations.corda.Sphincs
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.ksp.MetaInfServiceRegister
import com.ing.zkflow.ksp.filterConcreteClassesOrObjects
import com.ing.zkflow.ksp.getAllImplementedInterfaces
import com.ing.zkflow.ksp.getAnnotationsByType
import com.ing.zkflow.ksp.getSurrogateTargetClass
import com.ing.zkflow.ksp.implementsInterface
import com.ing.zkflow.ksp.implementsInterfaceDirectly
import com.ing.zkflow.ksp.upgrade.UpgradeCommandGenerator
import com.ing.zkflow.ksp.versioning.VersionFamilyGenerator
import com.ing.zkflow.ksp.versioning.VersionSorting
import com.ing.zkflow.ksp.versioning.VersionedCommandIdGenerator
import com.ing.zkflow.ksp.versioning.VersionedStateIdGenerator
import com.ing.zkflow.processors.SerializerProviderGenerator
import com.ing.zkflow.processors.SurrogateSerializerGenerator
import com.ing.zkflow.util.merge
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass

/**
 * This processor validates all classes in the CorDapp that are annotated with either @ZKP or @ZKPSurrogate.
 * It also generates some serialization logic for them, so that the fixed-length BFLSerializationScheme knows how to handle them.
 *
 * A strong assumption behind all logic in this processor is that for @ZKP or @ZKPSurrogate annotated classes, the properties of their
 * primary constructor are stable: i.e. they will not change ever again once they have been 'deployed'. They can change during development,
 * but never after deployment and usage in production. If they did, it would break deserialization of serialized classses.
 * If a change is required that would change the properties of the primary constructor, users should create a new version of that type.
 * This 'versioning' is left in the hands of the user for normal types. They can simply create new types when they need them.
 * ZKFLow will enforce that types never change after deployment. This ensures that when a user introduces a new version for a type that is
 * used in another user type, and they want to use it, they will also have to introduce a new type for that using type.
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
    private var builtinTypes: KSBuiltIns? = null

    private val metaInfServiceRegister = MetaInfServiceRegister(codeGenerator)

    private val serializerProviderGenerator = SerializerProviderGenerator(codeGenerator)
    private val upgradeCommandGenerator = UpgradeCommandGenerator(codeGenerator)
    private val versionFamilyGenerator = VersionFamilyGenerator(codeGenerator)
    private val surrogateSerializerGenerator = SurrogateSerializerGenerator(codeGenerator)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val newFiles = getNewFiles(resolver)

        builtinTypes = resolver.builtIns

        val zkpAnnotated = resolver.findClassesOrObjectsWithAnnotation(ZKP::class)
        val surrogates = getAllSurrogates(resolver)

        requireNoTopLevelSurrogates(surrogates)

        (zkpAnnotated + surrogates).forEach {
            // For now, objects are not allowed because the ZKFlow Arrow plugin currently does not support objects.
            // When Arrow is replaced with KSP, this check should be removed.
            // Associated task: https://dev.azure.com/INGNeo/ING%20Neo%20-%20ZKFlow/_workitems/edit/21489
            it.requireNotAnObject()

            it.requirePublicPrimaryConstructorWithOnlyPublicVals()
            it.requireNoGenerics()
            it.requireConstructorParamsSerializable()
            it.requireParentValsSafelyUsedInPrimaryConstructor()
        }

        val contractStateGroups = getSortedContractStateVersionGroups(newFiles, zkpAnnotated)

        val commands = VersionedCommandIdGenerator.generateIds(zkpAnnotated.filter { it.implementsInterface(CommandData::class) })
        val states = VersionedStateIdGenerator.generateIds(contractStateGroups)
        val upgradeCommands = upgradeCommandGenerator.generateUpgradeCommands(contractStateGroups.values)

        versionFamilyGenerator.generateFamilies(contractStateGroups).registerToServiceLoader()
        surrogateSerializerGenerator.generateSurrogateSerializers(surrogates)
        serializerProviderGenerator.generateProviders(states + commands + upgradeCommands).registerToServiceLoader()

        metaInfServiceRegister.emit()

        return emptyList()
    }

    /**
     * Surrogates are classes that are annotated with @ZKPSurrogate *and* implement [Surrogate].
     * Classes with only the annotation cause an error, classes only implementing the interface will be ignored.
     */
    private fun getAllSurrogates(resolver: Resolver): Sequence<KSClassDeclaration> {
        val annotated = resolver.findClassesOrObjectsWithAnnotation(ZKPSurrogate::class)
        val implementing = annotated.filter { it.implementsInterface(Surrogate::class) }

        require(annotated.count() == implementing.count()) {
            "All @${ZKPSurrogate::class.simpleName}-annotated classes should implement the ${Surrogate::class.simpleName} interface. " +
                "The following do not: ${(annotated - implementing).map { it.qualifiedName?.asString() }.joinToString(", ")}"
        }
        return implementing
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

    private fun KSClassDeclaration.requireNotAnObject() {
        require(this.classKind != ClassKind.OBJECT) {
            "${this.qualifiedName?.asString()} is an object. " +
                "For now, objects annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} are not allowed. " +
                "This is because the ZKFlow Arrow plugin currently does not support objects. When Arrow is replaced with KSP, this check should be removed."
        }
    }

    /**
     * To be able to serialize properly, @ZKP annotated classes must be public, must have a public primary constructor and all parameters
     * of the primary constructor must be val/var and public.
     */
    private fun KSClassDeclaration.requirePublicPrimaryConstructorWithOnlyPublicVals() {
        if (classKind == ClassKind.OBJECT) return

        requirePublic()
        val primaryConstructor = ensurePrimaryConstructor()
        requirePublicPrimaryConstructor(primaryConstructor)

        primaryConstructor.parameters.forEach {
            requireValVarParameter(it)
            requirePublicParameter(it)
        }
    }

    private fun KSClassDeclaration.requirePublic() {
        val nonPublic = listOf(Visibility.PRIVATE, Visibility.PROTECTED, Visibility.INTERNAL)
        require(getVisibility() !in nonPublic) {
            "Classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} " +
                "must be public. ${this.qualifiedName?.asString()} is ${getVisibility()}."
        }
    }

    private fun KSClassDeclaration.requirePublicParameter(it: KSValueParameter) {
        val nonPublic = listOf(Visibility.PRIVATE, Visibility.PROTECTED, Visibility.INTERNAL)
        val visibility =
            it.type.resolve().declaration.getVisibility()
        require(visibility !in nonPublic) {
            "All primary constructor parameters of classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} " +
                "must be public. Parameter $it of ${this.qualifiedName?.asString()} is $visibility."
        }
    }

    private fun KSClassDeclaration.requireValVarParameter(it: KSValueParameter) {
        require(it.isVal || it.isVar) {
            "All primary constructor parameters of classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} " +
                "must be a val or var. Parameter $it of ${this.qualifiedName?.asString()} is not."
        }
    }

    private fun KSClassDeclaration.requirePublicPrimaryConstructor(primaryConstructor: KSFunctionDeclaration) {
        val nonPublic = listOf(Visibility.PRIVATE, Visibility.PROTECTED, Visibility.INTERNAL)
        require(primaryConstructor.getVisibility() !in nonPublic) {
            "classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} must have a public primary constructor. " +
                "The constructor of ${this.qualifiedName?.asString()} is ${primaryConstructor.getVisibility()}."
        }
    }

    private fun KSClassDeclaration.ensurePrimaryConstructor(): KSFunctionDeclaration {
        return this.primaryConstructor ?: error(
            "classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} must have a primary constructor. " +
                "${qualifiedName?.asString()} does not."
        )
    }

    /**
     * At this time, we do not support generics on @ZKP/ZKPSurrogate annotated classes.
     * This may change in the future.
     */
    private fun KSClassDeclaration.requireNoGenerics() {
        require(typeParameters.isEmpty()) {
            "Classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} may not have type parameters. `$this` has parameters: ${typeParameters.map { it.simpleName.asString() }} " +
                "This may be supported in the future."
        }
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

    /**
     * We don't allow parent properties to be used in the constructor directly to prevent inadvertent
     * changes to the implementor by an upgrade of a dependency that introduces changes in that parent.
     *
     * Don't allow parent vals to be used in the constructor, unless there is a level of indirection:
     * - the val is annotated with @Via
     * - the val type itself is annotated with @ZKP
     */
    private fun KSClassDeclaration.requireParentValsSafelyUsedInPrimaryConstructor() {
        getAllSuperTypes().map { it.declaration }.filterIsInstance<KSClassDeclaration>()
            .forEach { parent -> requireParentValsNotUsedInPrimaryConstructor(this, parent) }
    }

    /**
     * We don't allow parent properties to be used in the constructor directly to prevent inadvertent
     * changes to the implementor by an upgrade of a dependency that introduces changes in that parent.
     *
     * Don't allow parent vals to be used in the constructor, unless there is a level of indirection:
     * - the val is annotated with @Via
     * - the val type itself is annotated with @ZKP
     */
    private fun requireParentValsNotUsedInPrimaryConstructor(implementor: KSClassDeclaration, parent: KSClassDeclaration) {
        // We allow direct usage of properties of certain core Corda classes, such as ContractState, OwnableState, etc.
        // These classes are considered stable unless the Corda platform version changes. ZKFlow is tied to a specific Corda platform version.
        if (parent.isConsideredStableCoreClass()) return
        val interfaceProperties = parent.getAllProperties().map { it.simpleName }
        val unsafeParams = implementor.primaryConstructor?.parameters?.filter {
            it.name in interfaceProperties &&
                !(it.type.isAnnotationPresent(Via::class) || it.type.resolve().declaration.isAnnotationPresent(ZKP::class))
        } ?: emptyList()

        require(unsafeParams.isEmpty()) {
            val paramNames = unsafeParams.mapNotNull { it.name?.asString() }.joinToString(", ")
            "Properties defined in parent $parent were unsafely overridden in primary constructor of ${implementor.qualifiedName?.asString()}: $paramNames. " +
                "There are three options to resolve this: 1) move them to the body of $implementor, " +
                "2) annotate the property type with @${Via::class.simpleName} or " +
                "3) annotate the the type itself where it is declared with @${ZKP::class.simpleName}"
        }
    }

    /**
     * We allow direct usage of properties of certain core Corda classes, such as ContractState, OwnableState, etc.
     * These classes are considered stable unless the Corda platform version changes. ZKFlow is tied to a specific Corda platform version.
     */
    private fun KSClassDeclaration.isConsideredStableCoreClass(): Boolean {
        return this.qualifiedName?.asString() in listOf(
            ContractState::class.qualifiedName,
            OwnableState::class.qualifiedName
        )
    }

    private fun KSClassDeclaration.requireConstructorParamsSerializable() {
        primaryConstructor?.parameters?.forEach {
            try {
                it.requireDirectlySerializable()
            } catch (e: Exception) {
                val errorPrefix =
                    "Constructor parameter '$it: ${it.type.renderRecursively()}' of ${this.qualifiedName?.asString()} is not serializable: "
                throw IllegalStateException(errorPrefix + e.message, e)
            }
        }
    }

    private fun KSTypeReference.renderRecursively(): String {
        var render = this.toString()
        val typeArgs = element?.typeArguments ?: emptyList()
        if (typeArgs.isNotEmpty()) {
            render += "<"
            render += typeArgs.map { it.type?.renderRecursively() }.joinToString(", ")
            render += ">"
        }
        return render
    }

    private fun KSValueParameter.requireDirectlySerializable() = type.requireSerializable()

    private fun KSTypeReference.requireSerializable() {
        // As soon as we encounter a @Via annotation, we stop checking all type arguments after that:
        // A @Via annotation implies that its surrogate covers the full type it applies to, including any type arguments.
        val viaAnnotation = getAnnotationsByType(Via::class).firstOrNull()
        if (viaAnnotation != null) {
            // TODO: should type checking of surrogate from @Via and of actual type it applies to be recursive?
            val surrogate =
                viaAnnotation.annotationType.element?.typeArguments?.singleOrNull()?.type?.resolve()?.declaration as? KSClassDeclaration
                    ?: error(
                        "Type ${this.resolve().declaration.qualifiedName?.asString()} is not serializable. " + "@${Via::class.simpleName} annotation must have a single type argument."
                    )
            val surrogateTarget = surrogate.getSurrogateTargetClass()
            require(this.resolve().declaration == surrogateTarget) {
                "Type ${this.resolve().declaration.qualifiedName?.asString()} is not serializable. " +
                    "Target '$surrogateTarget' of surrogate `$surrogate` set with @${Via::class.simpleName} annotation must match $this."
            }
            return
        }

        if (isZKFlowSupportedPrimitive() ||
            isZKFlowSupportedCoreClass() ||
            resolve().declaration.isAnnotationPresent(ZKP::class)
        ) {
            element?.typeArguments?.forEach { it.type?.requireSerializable() }
        } else {
            error(
                "Type ${this.resolve().declaration.qualifiedName?.asString()} is not serializable. " +
                    "Either add a @${Via::class.simpleName} annotation on $this, " +
                    "or ensure that the declaration of $this is annotated with @${ZKP::class.simpleName}."
            )
        }
    }

    private fun KSTypeReference.isZKFlowSupportedCoreClass(): Boolean {
        return when (val qualifiedName = this.resolve().declaration.qualifiedName?.asString()) {
            PublicKey::class.qualifiedName,
            AnonymousParty::class.qualifiedName,
            Party::class.qualifiedName -> {
                require(
                    isAnnotationPresent(EdDSA::class) ||
                        isAnnotationPresent(Sphincs::class) ||
                        isAnnotationPresent(EcDSA_R1::class) ||
                        isAnnotationPresent(EcDSA_K1::class) ||
                        isAnnotationPresent(RSA::class)
                ) {
                    "Missing algorithm annotation on $qualifiedName."
                }
                true
            }
            SecureHash::class.qualifiedName -> {
                require(annotations.any { it.annotationType.resolve().declaration.isAnnotationPresent(Algorithm::class) }) {
                    "Missing algorithm annotation on $qualifiedName."
                }
                true
            }
            BigDecimal::class.qualifiedName -> {
                require(isAnnotationPresent(BigDecimalSize::class)) {
                    "Missing @${BigDecimalSize::class.simpleName} annotation on $qualifiedName."
                }
                true
            }
            Map::class.qualifiedName, Set::class.qualifiedName, Collection::class.qualifiedName,
            List::class.qualifiedName, ByteArray::class.qualifiedName -> {
                require(isAnnotationPresent(Size::class)) {
                    "Missing @${Size::class.simpleName} annotation on $qualifiedName."
                }
                true
            }
            HashAttachmentConstraint::class.qualifiedName, SignatureAttachmentConstraint::class.qualifiedName,
            AlwaysAcceptAttachmentConstraint::class.qualifiedName, WhitelistedByZoneAttachmentConstraint::class.qualifiedName,
            AutomaticHashConstraint::class.qualifiedName, AutomaticPlaceholderConstraint::class.qualifiedName -> true
            CordaX500Name::class.qualifiedName, Instant::class.qualifiedName,
            StateRef::class.qualifiedName, TimeWindow::class.qualifiedName, UUID::class.qualifiedName -> true
            UByte::class.qualifiedName, UInt::class.qualifiedName, ULong::class.qualifiedName, UShort::class.qualifiedName -> true
            else -> false
        }
    }

    /**
     * Confirm that KSType is a primitive and that it is valid
     */
    @Suppress("ComplexMethod")
    private fun KSTypeReference.isZKFlowSupportedPrimitive(): Boolean {
        val builtinTypes = getBuiltinTypes()
        val resolved = this.resolve()

        return when {
            builtinTypes.charType.matches(resolved) -> {
                require(
                    isAnnotationPresent(ASCIIChar::class) ||
                        isAnnotationPresent(UnicodeChar::class)
                ) { "$this is a Char type and requires either a @${ASCIIChar::class.simpleName} annotation or a @${UnicodeChar::class.simpleName} annotation." }
                true
            }
            builtinTypes.stringType.matches(resolved) -> {
                require(
                    isAnnotationPresent(ASCII::class) ||
                        isAnnotationPresent(UTF8::class) ||
                        isAnnotationPresent(UTF16::class) ||
                        isAnnotationPresent(UTF32::class)
                ) {
                    "$this requires one of the following annotations: @${ASCII::class.simpleName}, " +
                        "@${UTF8::class.simpleName}, @${UTF16::class.simpleName} or @${UTF16::class.simpleName}."
                }
                true
            }
            builtinTypes.iterableType.matches(resolved) ||
                builtinTypes.arrayType.matches(resolved) -> {
                require(isAnnotationPresent(Size::class)) {
                    "$this is a collection/iterable type and requires a @${Size::class.simpleName} annotation."
                }
                true
            }
            builtinTypes.booleanType.matches(resolved) ||
                builtinTypes.byteType.matches(resolved) ||
                builtinTypes.intType.matches(resolved) ||
                builtinTypes.longType.matches(resolved) ||
                builtinTypes.shortType.matches(resolved) ||
                builtinTypes.floatType.matches(resolved) ||
                builtinTypes.doubleType.matches(resolved) -> true
            else -> false
        }
    }

    /**
     * For some reason same types are not always assignableFrom, but the declaration comparison is successful.
     * This has been observed for String:
     * When String is a `KSValueParameter.type` that is resolved, `builtinTypes.stringType.isAssignableFrom(resolved)` is true.
     * But when String KSTypeReference is taken from the type arguments of KSValueParameter.type, the same call can return false.
     */
    private fun KSType.matches(that: KSType): Boolean = isAssignableFrom(that) || this.declaration == that.declaration

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

    /**
     * Surrogates should not be or target so-called top-level classes in Corda. Those are the transaction components that go directly in
     * a transaction and that a user can create: [ContractState] and [CommandData].
     *
     * This is not allowed because there may be multiple surrogates defined for one target. In that case, at serialization time, it would
     * not be possible for ZKFlow to determine which one to use for serialization.
     *
     * This could be made possible by requiring all surrogates to be formally 'VersionedContractStateGroup' like [ContractState]. Then the serializer could
     * select the latest version of a surrogate for a type at serialization time. This was considered overkill at this time.
     */
    private fun requireNoTopLevelSurrogates(surrogates: Sequence<KSClassDeclaration>) {
        surrogates.forEach { surrogate ->
            surrogate.requireSurrogateNotImplements(ContractState::class)
            surrogate.requireSurrogateNotImplements(CommandData::class)

            val surrogateTarget = surrogate.getSurrogateTargetClass()

            surrogateTarget.requireSurrogateTargetNotImplements(surrogate, ContractState::class)
            surrogateTarget.requireSurrogateTargetNotImplements(surrogate, CommandData::class)
        }
    }

    private fun KSClassDeclaration.requireSurrogateNotImplements(interfaceKClass: KClass<*>) {
        require(!implementsInterface(interfaceKClass)) {
            "${qualifiedName?.asString()} is a ${interfaceKClass.simpleName} and should not be annotated with " +
                "@{${ZKPSurrogate::class.simpleName}. Annotate it with @${ZKP::class.simpleName} instead."
        }
    }

    private fun KSClassDeclaration.requireSurrogateTargetNotImplements(surrogate: KSClassDeclaration, interfaceKClass: KClass<*>) {
        require(!implementsInterface(interfaceKClass)) {
            "Surrogate target ${qualifiedName?.asString()} of surrogate ${surrogate.qualifiedName?.asString()} is a " +
                "${interfaceKClass.simpleName}. Surrogates should not target ${interfaceKClass.simpleName}s. Consider wrapping the target " +
                "in a @${ZKP::class.simpleName}-annotated class as a property that is annotated with @${Via::class.simpleName}"
        }
    }

    private fun Resolver.findClassesOrObjectsWithAnnotation(annotationKClass: KClass<out Annotation>): Sequence<KSClassDeclaration> {
        return getSymbolsWithAnnotation(annotationKClass.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filterConcreteClassesOrObjects()
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

    private fun getBuiltinTypes(): KSBuiltIns {
        return builtinTypes ?: error(
            "`builtinTypes` should have been initialized in `fun process(resolver: Resolver)` " +
                "by calling `builtinTypes = resolver.builtIns`"
        )
    }

    private class UnversionedException(message: String) : Exception(message)
}
