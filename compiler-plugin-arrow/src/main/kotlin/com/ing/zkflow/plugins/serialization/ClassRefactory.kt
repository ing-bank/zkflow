package com.ing.zkflow.plugins.serialization

import arrow.meta.phases.CompilerContext
import arrow.meta.quotes.Scope
import arrow.meta.quotes.classorobject.ClassDeclaration
import arrow.meta.quotes.scope
import com.ing.zkflow.SerdeLogger
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.plugins.serialization.serializingobject.SerializingObject
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtTypeReference

class ClassRefactory(
    private val classDeclaration: ClassDeclaration,
    private val ctx: CompilerContext
) {
    private val ktClass = classDeclaration.value
    private val typeResolver = BestEffortTypeResolver(ktClass.containingKtFile)

    companion object {
        /**
         * Verbosely verifies whether a ZKP annotation is applicable to the class.
         */
        fun verifyAnnotationCorrectness(ktClass: KtClass, short: Boolean = false): Boolean = with(ktClass) {
            SerdeLogger.phase("Validate class") { logger ->
                logger.log("Considering:\n`$text`", short)
                val applicability = (hasAnnotation<ZKP>() || hasAnnotation<ZKPSurrogate>()) && isCorrectClassTypeForZKPAnnotation()
                logger.log(if (applicability) "ACCEPTED" else "DISMISSED")
                applicability
            }
        }

        private fun KtClass.isCorrectClassTypeForZKPAnnotation(): Boolean {
            require(!(isAnnotation() || isInterface())) {
                "Review class definition `${this.name}`. Classes annotated with `${ZKP::class.simpleName}` or `${ZKPSurrogate::class.simpleName}` may not be annotation classes, interfaces or abstract classes"
            }

            require(typeParameters.isEmpty()) {
                "Review class definition `${this.name}`. Classes annotated with ${ZKP::class.simpleName} or `${ZKPSurrogate::class.simpleName}` may not contain generics"
            }

            return true
        }
    }

    val newDeclarations: List<Scope<KtElement>>
        get() = with(ctx) {
            val ktClass = classDeclaration.value
            ktClass.primaryConstructor?.let {
                listOf(buildClassDeclaration(it))
            } ?: listOf(
                "@${Serializable::class.qualifiedName}".annotationEntry,
                ktClass.scope()
            )
        }

    /**
     * Build a class redeclaration for a class with a non-empty primary constructor.
     */
    private fun buildClassDeclaration(primaryConstructor: KtPrimaryConstructor): ClassDeclaration =
        SerdeLogger.phase("Rebuilding class") { logger ->
            val (constructorParameters, serializingObjects) = primaryConstructor.valueParameters.fold(listOf<ConstructorParameter>()) { acc, ktParameter ->
                acc + buildConstructorParameter(ktParameter)
            }.let {
                val constructorParameters = it.joinToString(
                    prefix = "(${if (it.isEmpty()) "" else "\n\t"}",
                    separator = ",\n\t",
                    postfix = "${if (it.isEmpty()) "" else "\n"})"
                ) { item -> item.definition }

                val serializingObjects =
                    it.joinToString(separator = "\n\t") { item -> item.serializingObjects.joinToString(separator = "\n\t") }

                Pair(constructorParameters, serializingObjects)
            }

            // `ClassDeclaration.kind`'s implementation is Arrow Meta is buggy.
            // See https://github.com/arrow-kt/arrow-meta/pull/956
            // Version of Arrow cannot be bumped (17.12.2021) because it's under heavy development due to IR representation in Kotlin.
            val kind: Name = (
                when {
                    ktClass.isSealed() -> "sealed "
                    ktClass.isData() -> "data "
                    ktClass.hasModifier(KtTokens.OPEN_KEYWORD) -> "open "
                    else -> "/* empty? */"
                } +
                    ktClass.getClassOrInterfaceKeyword()?.text
                ).let(Name::identifier)

            with(ctx) {
                // BUG Simply printing out $`@annotations` in the new declaration is not reliable, prints only the first annotation.
                """
                |${classDeclaration.`@annotations`.value.joinToString(separator = "\n") { it.text }}
                |@${Serializable::class.qualifiedName}
                |$kind ${classDeclaration.name} ${classDeclaration.`(typeParameters)`} ${primaryConstructor.modifierList?.text.orEmpty()} ${constructorKeyword.text} ${constructorParameters.parameterList}
                |   ${if (classDeclaration.supertypes.isEmpty()) "" else ": ${classDeclaration.supertypes}"} {
                |   ${classDeclaration.body}
                |
                |   $serializingObjects
                |}
                """.`class`.also {
                    logger.phase("Update class definition") { logger ->
                        logger.log("Updating class parameter properties:\n`$it`")
                    }
                }
            }
        }

    /**
     * Given a parameter to a primary constructor, build its redefinition including a respective serialization
     * annotation and a sequence of serializing objects.
     */
    private fun buildConstructorParameter(ktParameter: KtParameter): ConstructorParameter = with(ktParameter) {
        SerdeLogger.phase("Building constructor parameter") { logger ->
            logger.log(ktParameter.text)

            if (!ktParameter.hasValOrVar()) {
                return@phase ConstructorParameter.Self(text)
            }

            val paramName = ktParameter.name?.capitalize() ?: error("Cannot infer the name of parameter $text")
            val annotatedType = ktParameter.typeReference ?: error("Cannot infer the type of $text")

            val serializingObject = buildSerializingObject(annotatedType)
            val support = serializingObject(paramName)

            val constructorParameter = ctx.ktPsiElementFactory.createProperty(
                modifierList?.text,
                name!!,
                serializingObject.redeclaration,
                isVarArg,
                defaultValue?.text
            ).let {
                "@${Serializable::class.qualifiedName}(with = ${support.serializer}) ${it.text}"
            }

            ConstructorParameter.Serializable(constructorParameter, support)
        }
    }

    /**
     * Recursively process the type tree to build a hierarchy of serializing objects.
     * Returns the root of the tree.
     */
    private fun buildSerializingObject(
        ktTypeReference: KtTypeReference,
        ignoreNullability: Boolean = false,
        depth: Int = 0
    ): SerializingObject = with(ktTypeReference) {
        val typeRef = ContextualizedKtTypeReference(this, typeResolver)

        val logMessage = typeRef.ktTypeElement.text.applyWhen(ignoreNullability) { removeSuffix("?") }
        SerdeLogger.phase("Building type `$logMessage`") { logger ->
            val resolvedRootType = typeRef.rootType.let {
                if (ignoreNullability) it.stripNullability() else it
            }

            logger.log("Root type has been resolved to `${resolvedRootType.type}`")
            // • Strip nullability.
            if (resolvedRootType.isNullable) {
                return@phase buildSerializingObject(this, ignoreNullability = true, depth + 1).wrapNull()
            }

            // • Invariant: root.isNullable = false

            if (Processors.isUserType(resolvedRootType.type)) {
                return@phase Processors.forOtherType(typeRef)
            }

            // • Strip outer type.
            val children = typeRef.ktTypeElement.typeArgumentsAsTypes.map {
                buildSerializingObject(it, ignoreNullability = false, depth + 1).let { so ->
                    if (resolvedRootType.isSupportedCollection) so.wrapDefault() else so
                }
            }

            Processors.forNativeType(ContextualizedKtTypeReference(this, typeResolver), children)
        }
    }
}
