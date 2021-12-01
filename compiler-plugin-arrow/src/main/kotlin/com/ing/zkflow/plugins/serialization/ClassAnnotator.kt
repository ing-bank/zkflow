package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.phases.CompilerContext
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import arrow.meta.quotes.scope
import com.ing.zkflow.SerdeLogger
import com.ing.zkflow.ZKP
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Inspired a lot by
 * https://speakerdeck.com/heyitsmohit/writing-kotlin-compiler-plugins-with-arrow-meta
 */

private const val PROCESSING_UNIT = "Classes Annotation"

/**
 * Annotates every constructor property with @Serializable and generate an appropriate sequence of serializing objects.
 */
val Meta.ClassAnnotator: CliPlugin
    get() = PROCESSING_UNIT {
        meta(
            classDeclaration(this, match = {
                element.verifyAnnotationCorrectness()
            }) { (ktClass, _) ->
                if (!ktClass.hasExplicitPrimaryConstructor()) {
                    Transform.replace(
                        replacing = ktClass,
                        newDeclarations = listOf(
                            "@${Serializable::class.qualifiedName}".annotationEntry,
                            ktClass.scope()
                        )
                    )
                } else {
                    // Invariant: Primary constructor is present.
                    val primaryConstructor = ktClass.primaryConstructor ?: error("Primary constructor for ${ktClass.name} must be present")

                    val (parameters, serializingObjects) = ktClass
                        .extractConstructorParameters(ctx)
                        .deconstructIntoDefinitionsAndSerializers()

                    Transform.replace(
                        replacing = ktClass,
                        // BUG Simply printing out $`@annotations` in the new declaration is not reliable, prints only the first annotation.
                        newDeclaration = """
                        |${`@annotations`.value.joinToString(separator = "\n") { it.text }}
                        |@${Serializable::class.qualifiedName}
                        |$kind $name $`(typeParameters)` ${primaryConstructor.modifierList?.text.orEmpty()} ${constructorKeyword.text} ${parameters.parameterList}
                        |   ${if (supertypes.isEmpty()) "" else ": $supertypes"} {
                        |   $body
                        |   
                        |   $serializingObjects
                        |}
                    """.`class`.also { SerdeLogger.log("Updating class parameter properties:\n$it") }
                    )
                }
            }
        )
    }

/**
 * Verbosely verifies whether a ZKP annotation is applicable to the class.
 */
private fun KtClass.verifyAnnotationCorrectness(): Boolean {
    SerdeLogger.log(PROCESSING_UNIT)
    SerdeLogger.log("Considering:\n$text")

    val applicability = hasAnnotation<ZKP>() && isCorrectClassTypeForZKPAnnotation()

    SerdeLogger.log("(CLASS) ${if (applicability) "SHALL" else "WILL NOT"} process")

    return applicability
}

private fun KtClass.extractConstructorParameters(ctx: CompilerContext): List<ConstructorParameter> =
    primaryConstructorParameters.fold(mutableListOf()) { acc, param ->
        acc += param.intoConstructorParameter(ctx)
        acc
    }

private fun KtParameter.intoConstructorParameter(ctx: CompilerContext): ConstructorParameter {
    if (!hasValOrVar()) {
        return ConstructorParameter.Self(text)
    }

    val paramName = name?.capitalize() ?: error("Cannot infer the name of parameter $text")

    val annotatedType = typeReference
    require(annotatedType != null) { "Cannot infer the type of $text" }

    val description = annotatedType.buildSerializingObjectsHierarchy()
    val support = description(paramName)

    val redefinition = ctx.ktPsiElementFactory.createProperty(
        modifierList?.text,
        name!!,
        description.redeclaration,
        isVarArg,
        defaultValue?.text
    ).let {
        "@${Serializable::class.qualifiedName}(with = ${support.serializer}) ${it.text}"
    }

    return ConstructorParameter.Serializable(redefinition, support)
}

private fun List<ConstructorParameter>.deconstructIntoDefinitionsAndSerializers(): Pair<String, String> {
    val parameters = joinToString(
        prefix = "(${if (isEmpty()) "" else "\n\t"}",
        separator = ",\n\t",
        postfix = "${if (isEmpty()) "" else "\n"})"
    ) { item -> item.definition }

    val serializingObjects =
        joinToString(separator = "\n\t") { item -> item.serializingObjects.joinToString(separator = "\n\t") }

    return Pair(parameters, serializingObjects)
}
