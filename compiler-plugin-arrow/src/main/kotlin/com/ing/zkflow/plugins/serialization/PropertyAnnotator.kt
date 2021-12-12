package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.property
import com.ing.zkflow.SerdeLogger
import com.ing.zkflow.ZKP
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.psi.KtProperty
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
            }) { (ktProperty, descriptor) ->
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

    val applicability = containingClassOrObject?.isOrdinaryClass ?: false &&
        containingClass()?.let {
            it.hasAnnotation<ZKP>() && it.isCorrectClassTypeForZKPAnnotation()
        } ?: false

    SerdeLogger.log("(PROP) ${if (applicability) "SHALL" else "WILL NOT"} process")

    return applicability
}
