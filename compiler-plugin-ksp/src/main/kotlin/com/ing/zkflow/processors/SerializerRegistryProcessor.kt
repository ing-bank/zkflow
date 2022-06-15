package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.common.serialization.KClassSerializer
import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class SerializerRegistryProcessor(private val codeGenerator: CodeGenerator) {

    fun process(implementations: Map<GeneratedSerializer, Int>): ServiceLoaderRegistration {
        val mapProviderClasses = implementations.entries
            .map { (implementationClass, implementationId) ->
                generateProvider(implementationClass, implementationId)
            }
        return ServiceLoaderRegistration(KClassSerializerProvider::class, mapProviderClasses)
    }

    private fun generateProvider(
        generatedSerializer: GeneratedSerializer,
        implementationId: Int,
    ): String {
        val className = "${generatedSerializer.className.simpleNames.joinToString("") { it }}SerializerProvider"
        FileSpec.builder(generatedSerializer.className.packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(KClassSerializerProvider::class)
                    .addFunction(
                        FunSpec.builder("get")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(KClassSerializer::class.asClassName().parameterizedBy(STAR))
                            .addCode(
                                buildCodeBlock {
                                    addStatement(
                                        "return %T(%L::class, %L, %L.serializer())",
                                        KClassSerializer::class,
                                        generatedSerializer.className,
                                        implementationId,
                                        generatedSerializer.className,
                                    )
                                }
                            )
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                aggregating = false,
                originatingKSFiles = generatedSerializer.sourceFiles
            )
        return "${generatedSerializer.className.packageName}.$className"
    }

    data class GeneratedSerializer(
        val className: ClassName,
        val sourceFiles: List<KSFile> = emptyList()
    ) {
        constructor(className: ClassName, sourceFile: KSFile?) : this(className, listOfNotNull(sourceFile))

        companion object {
            fun KSClassDeclaration.toImplementationClass(): GeneratedSerializer =
                GeneratedSerializer(toClassName(), containingFile)
        }
    }
}
