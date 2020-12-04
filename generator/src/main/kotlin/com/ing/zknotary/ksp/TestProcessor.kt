package com.ing.zknotary.ksp

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zknotary.annotations.ZKPState
import com.ing.zknotary.generator.FixedLengthType
import com.ing.zknotary.util.writeTo

@AutoService(SymbolProcessor::class)
class TestProcessor : SymbolProcessor {
    private lateinit var generator: CodeGenerator
    private lateinit var logger: KSPLogger

    override fun init(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) {
        generator = codeGenerator
        this.logger = logger
    }

    override fun process(resolver: Resolver) {
        resolver.getSymbolsWithAnnotation(ZKPState::class.qualifiedName!!)
            .asSequence()
            .filterIsInstance<KSClassDeclaration>()
            .forEach { clazz ->
                val file = FixedLengthType.fromClass(clazz)

                file.writeTo(generator)
            }
    }

    override fun finish() {}
}
