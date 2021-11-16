package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.property
import arrow.meta.quotes.scope
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
            }) { (ktProperty, _) ->
                Transform.replace<KtProperty>(
                    replacing = ktProperty,
                    newDeclarations = listOf(
                        Transient::class.qualifiedName!!.annotationEntry,
                        ktProperty.scope()
                    )
                ).also { SerdeLogger.log("Updating class properties:\n$it") }
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

    SerdeLogger.log("WILL${if (applicability) " " else " NOT "}process")

    return applicability
}
