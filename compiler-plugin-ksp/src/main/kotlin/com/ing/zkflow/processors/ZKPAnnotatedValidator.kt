@file:Suppress("DEPRECATION") // We need to support AutomaticHashConstraint

package com.ing.zkflow.processors

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Visibility
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
import com.ing.zkflow.annotations.ZKPStable
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.annotations.corda.Algorithm
import com.ing.zkflow.annotations.corda.SignatureSpec
import com.ing.zkflow.ksp.findClassesOrObjectsWithAnnotation
import com.ing.zkflow.ksp.getAllSurrogates
import com.ing.zkflow.ksp.getAnnotationsByType
import com.ing.zkflow.ksp.getSurrogateFromViaAnnotation
import com.ing.zkflow.ksp.getSurrogateTargetClass
import com.ing.zkflow.ksp.implementsInterface
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
 *
 * A strong assumption behind all logic in this processor is that for @ZKP or @ZKPSurrogate annotated classes, the properties of their
 * primary constructor are stable: i.e. they will not change ever again once they have been 'deployed'. They can change during development,
 * but never after deployment and usage in production. If they did, it would break deserialization of serialized classses.
 * If a change is required that would change the properties of the primary constructor, users should create a new version of that type.
 * This 'versioning' is left in the hands of the user for normal types. They can simply create new types when they need them.
 * ZKFLow will enforce that types never change after deployment. This ensures that when a user introduces a new version for a type that is
 * used in another user type, and they want to use it, they will also have to introduce a new type for that using type.
 */
class ZKPAnnotatedValidator(@Suppress("unused") private val logger: KSPLogger) : SymbolProcessor {
    private var builtinTypes: KSBuiltIns? = null

    override fun process(resolver: Resolver): List<KSAnnotated> {
        builtinTypes = resolver.builtIns

        val zkpAnnotated = resolver.findClassesOrObjectsWithAnnotation(ZKP::class)
        val surrogates = resolver.getAllSurrogates()

        requireNoTopLevelSurrogates(surrogates)
        requireNoConcreteZKPStableAnnotated(resolver)

        (zkpAnnotated + surrogates).forEach {
            // For now, objects are not supported.
            // Associated task: https://dev.azure.com/INGNeo/ING%20Neo%20-%20ZKFlow/_workitems/edit/21489
            it.requireNotAnObject()

            it.requirePublicPrimaryConstructorWithOnlyPublicVals()
            it.requireNoGenerics()
            it.requireConstructorParamsSerializable()
            it.requireParentValsSafelyUsedInPrimaryConstructor()
        }

        return emptyList()
    }

    private fun requireNoConcreteZKPStableAnnotated(resolver: Resolver) {
        resolver.findClassesOrObjectsWithAnnotation(ZKPStable::class).forEach {
            require(it.isAbstract()) {
                "Types annotated with @${ZKPStable::class.simpleName} should be an interface or abstract class. " +
                    "${it.qualifiedName?.asString()} is: ${it.classKind.name}"
            }
        }
    }

    private fun KSClassDeclaration.requireNotAnObject() {
        require(this.classKind != ClassKind.OBJECT) {
            "${this.qualifiedName?.asString()} is an object. " +
                "For now, objects annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} are not supported. Please use a class with an empty primary constructor instead."
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
        require(getVisibility() == Visibility.PUBLIC) {
            "Classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} " +
                "must be public. ${this.qualifiedName?.asString()} is ${getVisibility()}."
        }
    }

    private fun KSClassDeclaration.requirePublicParameter(parameter: KSValueParameter) {
        val matchingProperty = this.getDeclaredProperties().single { it.simpleName == parameter.name }
        val visibility = matchingProperty.getVisibility()
        require(visibility == Visibility.PUBLIC) {
            "All primary constructor parameters of classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} " +
                "must be public. Parameter $parameter of ${this.qualifiedName?.asString()} is not."
        }
    }

    private fun KSClassDeclaration.requireValVarParameter(it: KSValueParameter) {
        require(it.isVal || it.isVar) {
            "All primary constructor parameters of classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} " +
                "must be a val or var. Parameter '$it' of ${this.qualifiedName?.asString()} is not."
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
            "Classes annotated with @${ZKP::class.simpleName} or @${ZKPSurrogate::class.simpleName} may not have type parameters. " +
                "`${this.qualifiedName?.asString()}` has parameters: ${typeParameters.map { it.simpleName.asString() }}."
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
     * - the parent type is considered 'stable'. This is true for types annotated with @ZKPStable or @ZKP.
     */
    private fun requireParentValsNotUsedInPrimaryConstructor(implementor: KSClassDeclaration, parent: KSClassDeclaration) {
        if (parent.isConsideredStableParentClass()) return

        val parentProperties = parent.getDeclaredProperties().map { it.simpleName }
        val unsafeParams = implementor.primaryConstructor?.parameters?.filter {
            it.name in parentProperties && !it.type.isAnnotationPresent(Via::class)
        } ?: emptyList()

        require(unsafeParams.isEmpty()) {
            val paramNames = unsafeParams.mapNotNull { it.name?.asString() }.joinToString(", ")
            "Properties defined in parent $parent were unsafely overridden in primary constructor of ${implementor.qualifiedName?.asString()}: $paramNames. " +
                "There are three options to resolve this: 1) move them to the body of $implementor, " +
                "2) annotate the property type with @${Via::class.simpleName} or " +
                "3) ensure that the parent is considered 'stable', i.e. is annotated with @${ZKP::class.simpleName}, @${ZKPStable::class.simpleName} or  @${ZKPSurrogate::class.simpleName}"
        }
    }

    /**
     * We allow direct usage of properties of certain parent classes only if they are:
     * - stable core Corda classes, such as ContractState, OwnableState, etc.
     * - annotated with @ZKPStable or @ZKP.
     *
     * These classes are considered stable, i.e. never changing.
     * For Corda classes this is true unless the Corda platform version changes.
     * ZKFlow is already tied to a specific Corda platform version.
     */
    private fun KSDeclaration.isConsideredStableParentClass(): Boolean {
        return this.isZKPStable() || this.qualifiedName?.asString() in listOf(
            ContractState::class.qualifiedName,
            OwnableState::class.qualifiedName
        )
    }

    /**
     * True for any class that is annotated with ZKPStable (or indirectly annotated through ZKP or ZKPSurrogate).
     */
    private fun KSAnnotated.isZKPStable(): Boolean {
        return this.isAnnotationPresent(ZKPStable::class) ||
            this.isAnnotationPresent(ZKP::class) ||
            this.isAnnotationPresent(ZKPSurrogate::class)
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

    private fun KSTypeReference.renderRecursively(): String =
        this.toString() + element?.typeArguments.orEmpty().joinToString(separator = ", ") {
            it.type?.renderRecursively().orEmpty()
        }.let { if (it.isNotBlank()) "<$it>" else "" }

    private fun KSValueParameter.requireDirectlySerializable() = type.requireSerializable()

    private fun KSTypeReference.requireSerializable() {
        // As soon as we encounter a @Via annotation, we stop checking all type arguments after that:
        // A @Via annotation implies that its surrogate covers the full type it applies to, including any type arguments.
        // TODO: should type checking of surrogate from @Via and of actual type it applies to be recursive?
        val viaAnnotation = getAnnotationsByType(Via::class).firstOrNull()
        if (viaAnnotation != null) {
            val surrogate = viaAnnotation.getSurrogateFromViaAnnotation()
            val surrogateTarget = surrogate.getSurrogateTargetClass()

            require(this.resolve().declaration == surrogateTarget) {
                "Type ${this.resolve().declaration.qualifiedName?.asString()} is not serializable. " +
                    "Target type of surrogate `$surrogate` set with @${Via::class.simpleName} annotation must be $this, but is '$surrogateTarget'."
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
            SignatureAttachmentConstraint::class.qualifiedName,
            PublicKey::class.qualifiedName,
            AnonymousParty::class.qualifiedName,
            Party::class.qualifiedName -> {
                require(annotations.any { it.annotationType.resolve().declaration.isAnnotationPresent(SignatureSpec::class) }) {
                    "Missing algorithm annotation on $qualifiedName."
                }
                true
            }
            HashAttachmentConstraint::class.qualifiedName,
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
                "@${ZKPSurrogate::class.simpleName}. Annotate it with @${ZKP::class.simpleName} instead."
        }
    }

    private fun KSClassDeclaration.requireSurrogateTargetNotImplements(surrogate: KSClassDeclaration, interfaceKClass: KClass<*>) {
        require(!implementsInterface(interfaceKClass)) {
            "Surrogate target ${qualifiedName?.asString()} of surrogate ${surrogate.qualifiedName?.asString()} is a " +
                "${interfaceKClass.simpleName}. Surrogates should not target ${interfaceKClass.simpleName}s."
        }
    }

    private fun getBuiltinTypes(): KSBuiltIns {
        return builtinTypes ?: error(
            "`builtinTypes` should have been initialized in `fun process(resolver: Resolver)` " +
                "by calling `builtinTypes = resolver.builtIns`"
        )
    }
}
