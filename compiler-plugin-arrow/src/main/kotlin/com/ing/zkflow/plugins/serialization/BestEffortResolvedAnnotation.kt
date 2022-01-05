package com.ing.zkflow.plugins.serialization

import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * This class wraps a resolved annotation, it can be just a [KtAnnotationEntry] if the annotation is present
 * in the user code, or an actual instance of an annotation class [T] if the annotation is present in the compiled
 * dependency code.
 */
sealed class BestEffortResolvedAnnotation(val root: String) {
    class Instruction(rootType: String, val annotation: KtAnnotationEntry) : BestEffortResolvedAnnotation(rootType) {
        override fun toString(): String = "Instruction($root, ${annotation.text})"
    }

    class Compiled<T>(rootType: String, val annotation: T) : BestEffortResolvedAnnotation(rootType) {
        override fun toString(): String = "Compiled($root, $annotation)"
    }
}
