package com.ing.zkflow.plugins.serialization

import arrow.meta.phases.CompilerContext
import arrow.meta.quotes.classorobject.ClassDeclaration
import arrow.meta.quotes.element.ClassBody
import arrow.meta.quotes.orEmpty
import com.ing.zkflow.SerdeLogger
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.plugins.serialization.serializingobject.SerializingObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
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

    val newDeclaration =
        classDeclaration.value.rebuildDeclaration()

    /**
     * Re-build a class redeclaration adding appropriate annotations.
     */
    @Suppress("LongMethod", "UnusedPrivateMember")
    private fun KtClass.rebuildDeclaration(): ClassDeclaration =
        SerdeLogger.phase("Rebuilding class") { logger ->
            val (constructorParameters, serializingObjects) = primaryConstructor
                ?.valueParameters
                ?.fold(listOf<ConstructorParameter>()) { acc, ktParameter ->
                    acc + buildConstructorParameter(ktParameter)
                }
                ?.let {
                    val constructorParameters = it.joinToString(
                        prefix = if (it.isEmpty()) "" else "\n\t",
                        separator = ",\n\t",
                        postfix = if (it.isEmpty()) "" else "\n"
                    ) { item -> item.definition }

                    val serializingObjects =
                        it.joinToString(separator = "\n\t") { item -> item.serializingObjects.joinToString(separator = "\n\t") }

                    Pair(constructorParameters, serializingObjects)
                }
                ?: Pair("", "")

            // `ClassDeclaration.kind`'s implementation is Arrow Meta is buggy.
            // See https://github.com/arrow-kt/arrow-meta/pull/956
            // Version of Arrow cannot be bumped (17.12.2021) because it's under heavy development due to IR representation in Kotlin.
            val kind: Name = (
                when {
                    ktClass.isSealed() -> "sealed "
                    ktClass.isData() -> "data "
                    ktClass.hasModifier(KtTokens.OPEN_KEYWORD) -> "open "
                    ktClass.isEnum() -> "enum "
                    else -> "/* empty? */"
                } +
                    ktClass.getClassOrInterfaceKeyword()?.text
                ).let(Name::identifier)

            // We regenerate the full class body and attach @kotlinx.serialization.Transient annotations to all properties.
            // Unfortunately the decomposition ( treat properties + treat constructor parameters ) is not robust and removes
            // ZKP annotation from constructor paramaters as the following demonstrates
            // data class A(val list: @Size(5) List<@EdDSA Party>) {
            //      val a:Int = 100
            // }
            // -->
            // data class A(val list: @Size(5) List<Party>) {
            //      @kotlinx.serialization.Transient
            //      val a:Int = 100
            // }
            // It is unclear what the reasons for this behaviour are; no bug report is submitted because
            // - Arrow Meta moves towards working with IR representation (see discussion, https://github.com/arrow-kt/arrow-meta/pull/956)
            // - We also want to do away with Arrow Meta and redesign ZKP annotation processing
            //   to generate new code serializing code instead of altering the existing code.
            //
            // The `buildBody` function is designed after `arrow.meta.quotes.element.ClassBody.identity`

            val classBody = buildBody(classDeclaration.body)

            with(ctx) {
                // BUG Simply printing out $`@annotations` in the new declaration is not reliable, prints only the first annotation.
                """
                |${classDeclaration.`@annotations`.value.joinToString(separator = "\n") { it.text }}
                |@${Serializable::class.qualifiedName}
                |$kind ${classDeclaration.name} ${classDeclaration.`(typeParameters)`} ${primaryConstructor?.modifierList?.text.orEmpty()} ${constructorKeyword.text}($constructorParameters)
                |   ${if (classDeclaration.supertypes.isEmpty()) "" else ": ${classDeclaration.supertypes}"} {
                |   
                |   $classBody
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

    @Suppress("UnusedPrivateMember")
    private fun buildBody(body: ClassBody): String = with(body) {
        SerdeLogger.phase("Re-building class body") { logger ->
            if (properties.isEmpty()) {
                logger.log("Body has no properties. Returning it as is.")
                "$body"
            } else {
                // else rebuild the class
                logger.log("Body has properties. Annotating as Transient if applicable")

                val annotatedProperties = properties.value.joinToString(separator = "\n") { ktProperty ->
                    logger.log("Updating:\n\t${ktProperty.text}")

                    if (ktProperty.hasNoBackingField()) {
                        logger.log("Property has no backing field, no annotation required")
                        return@joinToString ktProperty.text
                    }

                    // Left for future references.
                    //
                    // Arrow has troubles with comments and doc strings, remove them altogether.
                    // This approach has a small risk of removing too much, causing compilation errors.
                    // On the other hand, leaving comments in has caused issues in the past.
                    // val propertyClearDeclaration = ktProperty.docComment?.let { kDoc ->
                    //     ktProperty.text
                    //         .replace(kDoc.text, "")
                    //         .trimIndent()
                    //         .lines()
                    //         .filterNot { it.isBlank() }
                    //         .joinToString(separator = "\n")
                    // } ?: ktProperty.text

                    """
                       ${"@${Transient::class.qualifiedName!!}"}
                       ${ktProperty.text}
                       """
                        .trimIndent()
                        .also { logger.log("Updated property:\n$it") }
                }

                """
                    ${if (enumEntries.isEmpty()) "" else "$enumEntries"}
                
                    $annotatedProperties
                
                    $companionObjects
                
                    $functions
                    """
                    .trimIndent()
                    .also { logger.log("Updated class body:\n$it") }
            }
        }
    }

    private fun KtProperty.hasNoBackingField() = !hasBackingField()

    private fun KtProperty.hasBackingField(): Boolean {
        val getsField = (getter?.bodyExpression ?: getter?.bodyBlockExpression)?.isNotConst() ?: true
        val setsField = (setter?.bodyExpression ?: setter?.bodyBlockExpression)?.isNotConst() ?: true

        return getsField && setsField
    }

    /**
     * A sub-optimal way to deduce whether expression is a constant.
     * For proper analysis, consult with
     * https://github.com/JetBrains/intellij-community/pull/1678/files#diff-18106261245e00b0e9cf958fc4b077006359069ad528075566509418d11d9e57R154
     * However, that analysis uses BindingContext which is
     * (it seems to me, will be happy if someone proves me wrong)
     * inaccessible here.
     */
    private fun KtExpression.isNotConst(): Boolean {
        SerdeLogger.log(text)
        return when (this) {
            is KtConstantExpression -> false
            is KtStringTemplateExpression -> this.hasInterpolation()
            else -> {
                val fieldReferences = "^[\\s\\S&&[^/]]*field.*\$".toRegex()
                // E.g., on the second one will match
                // |  // field = 2
                // |    ({field = 2})  //
                // | x = 2 // field = 2
                this.text.contains(fieldReferences)
            }
        }
    }
}
