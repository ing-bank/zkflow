package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.SerdeLogger
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Via
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtTypeReference
import kotlin.reflect.KClass

/**
 * Class attaching type-resolution context for a [KtTypeReference]
 */
class ContextualizedKtTypeReference(
    /**
     * TypeReference may be a reference to a single native type (as defined in [Processors])
     * and, in case of user types, a generic parametrized type, e.g., @SomeAnnotation SomeUserType<@SomeAnnotation AnotherUserType>
     */
    val ktTypeReference: KtTypeReference,
    val typeResolver: BestEffortTypeResolver
) {
    val ktTypeElement: KtTypeElement by lazy {
        ktTypeReference.typeElement ?: error("Cannot infer type of: ${ktTypeReference.text}")
    }

    val text: String = ktTypeReference.text

    val rootType: ContextualType by lazy {
        with(ktTypeElement) {
            // KtTypeElement does not have annotations attached to the outer type.
            // Its typeArguments are TypeReferences, their text representation does contain annotations.
            typeArgumentsAsTypes
                .fold(text) { acc, arg -> acc.replace(arg.text, "") }
                .replace("[<>, \n\t]".toRegex(), "")
                .let {
                    val isNullable = it.last() == '?'

                    // Drop the last `?` if present.
                    val simpleName = if (isNullable) it.substring(0, it.lastIndex) else it

                    ContextualType(typeResolver.resolve(simpleName), isNullable)
                }
        }
    }

    /**
     * Derive a type declaration free of annotations
     */
    val cleanTypeDeclaration: String by lazy {
        stripAnnotations(ignoreNullability = false)
    }

    /**
     * Resolve a [KtElement] within the context of this [ktTypeReference].
     */
    fun resolveClass(argument: KtElement): BestEffortResolvedType = with(argument.text) {
        SerdeLogger.log("Resolving $this")
        val simpleName = trim().replace("::class", "").trimStart('@').trim()

        typeResolver.resolve(simpleName).also {
            SerdeLogger.log("Resolved to $it")
        }
    }

    /**
     * Recursively attach the given annotation A.
     * E.g. for  `List<@Annotation CustomType<Int>>`:
     * 1. `List<@Annotation CustomType<Int>>` -> @A List
     * 2. `CustomType<Int>` -> @A CustomType
     * 3. `Int` -> @A Int
     * =. @A List<@A CustomType<@A Int>>
     */
    fun attachAnnotation(annotation: KClass<*>, ignoreNullability: Boolean): String {
        val annotationsDeclaration = (
            ktTypeReference.annotationEntries.map { it.text } + "@${annotation.qualifiedName!!}"
            ).joinToString(separator = " ")

        val nullability = if (ignoreNullability) "" else if (rootType.isNullable) "?" else ""

        if (ktTypeElement.typeArgumentsAsTypes.isEmpty()) {
            // Early exit, there are no further subtypes to attach annotations to.
            return "$annotationsDeclaration ${rootType.type}$nullability"
        }

        // Attach annotations to the inner types.
        return "$annotationsDeclaration ${rootType.type}${
        ktTypeElement.typeArgumentsAsTypes.joinToString(
            prefix = "<",
            separator = ", ",
            postfix = ">"
        ) { ContextualizedKtTypeReference(it, typeResolver).attachAnnotation(annotation, ignoreNullability = false) }
        }$nullability"
    }

    /**
     * Recursively build the clean type tree.
     * E.g. for  `List<@Annotation CustomType<Int>>`:
     * 1. `List<@Annotation CustomType<Int>>` -> List
     * 2. `CustomType<Int>` -> CustomType
     * 3. `Int` -> Int
     * =. List<CustomType<Int>>
     */
    fun stripAnnotations(ignoreNullability: Boolean): String {
        val nullability = if (ignoreNullability) "" else if (rootType.isNullable) "?" else ""

        if (ktTypeElement.typeArgumentsAsTypes.isEmpty()) {
            // Early exit, there are no further subtypes to strip annotations from.
            return "${rootType.type}$nullability"
        }

        // Strip annotations from the inner types.
        return "${rootType.type}${
        ktTypeElement.typeArgumentsAsTypes.joinToString(
            prefix = "<",
            separator = ", ",
            postfix = ">"
        ) { ContextualizedKtTypeReference(it, typeResolver).stripAnnotations(ignoreNullability = false) }
        }$nullability"
    }

    inline fun <reified T> annotationSingleArgument(): String? = ktTypeReference.annotationSingleArgument<T>()

    inline fun <reified T> findAnnotation(): KtAnnotationEntry? = ktTypeReference.findAnnotation<T>()

    /**
     * Finds a surrogate serializer for this [ktTypeReference].
     *
     * We _naively_ expect that if there was a correct surrogate,
     * a relevant serializer has been generated by KSP and
     * placed in package [Surrogate.GENERATED_SURROGATE_SERIALIZER_PACKAGE_NAME] and
     * named "SurrogateClassName[Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX]"
     */
    fun findSurrogateSerializer(): FqName? {
        SerdeLogger.log("Looking for a surrogate specification for `${ktTypeElement.text}`")

        return ktTypeReference.findAnnotation<Via<*>>()?.let {
            val surrogate = it.typeArguments.single()
            val surrogateFqName = when (val resolved = resolveClass(surrogate)) {
                is BestEffortResolvedType.FullyResolved -> resolved.fqName
                is BestEffortResolvedType.FullyQualified -> resolved.fqName
                is BestEffortResolvedType.AsIs -> error("Cannot resolve `${surrogate.text}`")
            }

            val surrogateSerializerFqName = FqName.fromSegments(
                Surrogate.GENERATED_SURROGATE_SERIALIZER_PACKAGE_NAME.split(".") +
                    "${surrogateFqName.shortName().identifier}${Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX}"
            )

            SerdeLogger.log("[Expected] Surrogate serializer: `$surrogateSerializerFqName`")

            surrogateSerializerFqName
        }
    }

    /**
     * - Inspect annotations of [ktTypeReference],
     * - resolve it,
     * - look for a specified annotation `T` of the resolved annotation,
     * - combine them into a list and return.
     *
     * Searches only 1 level deep.
     */
    internal inline fun <reified T : Annotation> findMetaAnnotation(): List<BestEffortResolvedAnnotation> {
        SerdeLogger.log(
            """
            Inspecting `${ktTypeReference.text}`:
                annotations: [${ktTypeReference.annotationEntries.joinToString(separator = ", ") { it.text }}]
            Looking for a meta-annotation ${T::class.qualifiedName!!} 
            """.trimIndent()
        )

        return ktTypeReference
            .annotationEntries
            .mapNotNull { annotation ->
                when (val resolved = resolveClass(annotation)) {
                    is BestEffortResolvedType.AsIs -> null
                    is BestEffortResolvedType.FullyQualified -> {
                        resolved.findAnnotation<T>()?.let { ktAnnotationEntry ->
                            BestEffortResolvedAnnotation.Instruction("${resolved.fqName}", ktAnnotationEntry)
                        }
                    }
                    is BestEffortResolvedType.FullyResolved -> {
                        resolved.findAnnotation<T>()?.let { type ->
                            BestEffortResolvedAnnotation.Compiled("${resolved.fqName}", type)
                        }
                    }
                }
            }.also {
                SerdeLogger.log("Meta annotations: [${it.joinToString {ma -> "$ma" }}]")
            }
    }
}

data class ContextualType(val bestEffortResolvedType: BestEffortResolvedType, val isNullable: Boolean) {
    fun stripNullability(): ContextualType = copy(isNullable = false)

    val type = bestEffortResolvedType.asString()

    val isSupportedCollection: Boolean by lazy {
        type in Processors.genericCollections.keys
    }
}
