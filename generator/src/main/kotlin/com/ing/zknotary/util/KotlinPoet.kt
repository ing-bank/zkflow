package com.ing.zknotary.util

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.FileSpec
import java.io.OutputStreamWriter

fun FileSpec.writeTo(codeGenerator: CodeGenerator) {
    val file = codeGenerator.createNewFile(packageName, name)
    // Don't use writeTo(file) because that tries to handle directories under the hood
    OutputStreamWriter(file).use(::writeTo)
}
