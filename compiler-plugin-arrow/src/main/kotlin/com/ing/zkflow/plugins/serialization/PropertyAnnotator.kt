package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.property
import com.ing.zkflow.SerdeLogger
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.util.isOrdinaryClass

private const val PROCESSING_UNIT = "PROPERTY ANNOTATOR"

/**
 * Annotates all non-constructor properties as @Transient  to exclude them from serialization.
 */
val Meta.PropertyAnnotator: CliPlugin
    get() = PROCESSING_UNIT {
        meta(
            property(this, match = {
                SerdeLogger.phase(PROCESSING_UNIT) {
                    element.verifyAnnotationCorrectness()
                }
            }) { (ktProperty, _) ->
                SerdeLogger.phase(PROCESSING_UNIT) { logger ->
                    // Arrow has troubles with comments and doc strings, remove them altogether.
                    val allComments =
                        "((['\"])(?:(?!\\2|\\\\).|\\\\.)*\\2)|\\/\\/[^\\n]*|\\/\\*(?:[^*]|\\*(?!\\/))*\\*\\/".toRegex()
                    val propertyClearDeclaration = ktProperty.text
                        .replace(allComments, "")
                        .trimIndent()
                        .lines()
                        .filterNot { it.isBlank() }
                        .joinToString(separator = "\n")

                    Transform.replace(
                        replacing = ktProperty,
                        newDeclaration = """
                        ${"@${Transient::class.qualifiedName!!}".annotationEntry}
                        $propertyClearDeclaration
                        """.trimIndent().property(descriptor).also {
                            logger.phase("Update class property") { logger ->
                                logger.log("$it")
                            }
                        }
                    )
                }
            }
        )
    }

/**
 * Verbosely verifies whether the property is a part of a ZKP annotated class.
 */
private fun KtProperty.verifyAnnotationCorrectness(): Boolean = SerdeLogger.phase("Validate property") { logger ->
    logger.log("Considering:\n`$text`")

    val applicability = (containingClassOrObject?.isOrdinaryClass ?: false) &&
        (
            containingClass()?.let {
                SerdeLogger.log("Examine the parent class")
                ClassRefactory.verifyAnnotationCorrectness(it)
            } ?: false
            ) &&
        hasBackingField()

    logger.log(if (applicability) "ACCEPTED" else "DISMISSED")
    applicability
}

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
