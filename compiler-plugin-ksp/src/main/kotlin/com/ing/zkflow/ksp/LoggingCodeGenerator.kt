package com.ing.zkflow.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import java.io.OutputStream

class LoggingCodeGenerator private constructor(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) : CodeGenerator by codeGenerator {
    constructor(environment: SymbolProcessorEnvironment) : this(environment.logger, environment.codeGenerator)
    override fun createNewFile(
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        extensionName: String
    ): OutputStream {
        when (extensionName) {
            "kt" -> logger.info("Generating File: $packageName.$fileName.$extensionName")
            else -> logger.info("Generating File: $packageName/$fileName.$extensionName")
        }
        return codeGenerator.createNewFile(dependencies, packageName, fileName, extensionName)
    }
}
