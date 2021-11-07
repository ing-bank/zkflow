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

@Suppress("TopLevelPropertyNaming")
private const val processingUnit = "Property Annotator"

/**
 * Annotates all non-constructor properties as @Transient  to exclude them from serialization.
 */
val Meta.PropertyAnnotator: CliPlugin
    get() = processingUnit {
        meta(
            property(this, match = { element.validate() }) { (ktProperty, _) ->
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

private fun KtProperty.validate(): Boolean {
    SerdeLogger.log(processingUnit)
    SerdeLogger.logShort("Considering:\n$text")

    val applicability = containingClassOrObject?.isOrdinaryClass ?: false &&
        containingClass()
            ?.let {
                if (it.hasAnnotation<ZKP>()) { it.verifyZKP(); true } else { false }
            } ?: false

    SerdeLogger.log("WILL${if (applicability) " " else " NOT "}process")

    return applicability
}
