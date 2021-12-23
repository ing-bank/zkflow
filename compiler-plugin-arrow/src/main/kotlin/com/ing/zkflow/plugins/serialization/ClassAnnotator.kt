package com.ing.zkflow.plugins.serialization

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.quotes.Transform
import arrow.meta.quotes.classDeclaration

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
                ClassRefactory.verifyAnnotationCorrectness(PROCESSING_UNIT, element)
            }) { (ktClass, _) ->
                Transform.replace(
                    replacing = ktClass,
                    newDeclarations = ClassRefactory(this, ctx).newDeclarations
                )
            }
        )
    }
