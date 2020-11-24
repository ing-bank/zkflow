package com.ing.zknotary.util

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

class PropertyDescriptor (
    val name: String,
    val type: TypeName,
    val fromInstance: CodeBlock,
    val default: CodeBlock
) {
    fun debug(logger: KSPLogger) {
        logger.error("$name : $type")
        logger.error("$fromInstance")
        logger.error("$default")
    }
}