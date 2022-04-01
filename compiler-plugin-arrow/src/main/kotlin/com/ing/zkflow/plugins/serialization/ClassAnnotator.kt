package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration
import com.ing.zkflow.SerdeLogger

/**
 * Inspired a lot by
 * https://speakerdeck.com/heyitsmohit/writing-kotlin-compiler-plugins-with-arrow-meta
 */

private const val PROCESSING_UNIT = "CLASS ANNOTATOR"

/**
 * Annotates every constructor property with @Serializable and generate an appropriate sequence of serializing objects.
 */
val Meta.ClassAnnotator: CliPlugin
    get() = PROCESSING_UNIT {
        meta(
            classDeclaration(this, match = {
                SerdeLogger.phase(PROCESSING_UNIT) {
                    ClassRefactory.verifyAnnotationCorrectness(element)
                }
            }) { (ktClass, _) ->
                try {
                    SerdeLogger.phase(PROCESSING_UNIT) {
                        Transform.replace(
                            replacing = ktClass,
                            newDeclarations = ClassRefactory(this, ctx).newDeclarations
                        )
                    }
                } catch (e: Exception) {
                    throw IllegalStateException("Error while processing ${ktClass.nameAsSafeName}: ${e.message}", e)
                }
            }
        )
    }
