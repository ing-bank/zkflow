package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.property
import com.ing.zkflow.SerdeLogger
import com.ing.zkflow.ZKP
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.util.isOrdinaryClass

private const val PROCESSING_UNIT = "Property Annotator"

/**
 * Annotates all non-constructor properties as @Transient  to exclude them from serialization.
 */
val Meta.PropertyAnnotator: CliPlugin
    get() = PROCESSING_UNIT {
        meta(
            property(this, match = {
                element.verifyAnnotationCorrectness()
            }) { (ktProperty, _) ->
                // Arrow has troubles with comments and doc strings, remove them altogether.
                val allComments = "((['\"])(?:(?!\\2|\\\\).|\\\\.)*\\2)|\\/\\/[^\\n]*|\\/\\*(?:[^*]|\\*(?!\\/))*\\*\\/".toRegex()
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
                    """.trimIndent().property(descriptor).also { SerdeLogger.log("Updating class properties:\n$it") }
                )
            }
        )
    }

/**
 * Verbosely verifies whether the property is a part of a ZKP annotated class.
 */
private fun KtProperty.verifyAnnotationCorrectness(): Boolean {
    SerdeLogger.log(PROCESSING_UNIT)
    SerdeLogger.logShort("Considering:\n$text")

    val applicability = (containingClassOrObject?.isOrdinaryClass ?: false) &&
        (containingClass()?.let { it.hasAnnotation<ZKP>() && it.isCorrectClassTypeForZKPAnnotation() } ?: false) &&
        hasBackingField()

    SerdeLogger.log("(PROP) ${if (applicability) "SHALL" else "WILL NOT"} process")

    return applicability
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
