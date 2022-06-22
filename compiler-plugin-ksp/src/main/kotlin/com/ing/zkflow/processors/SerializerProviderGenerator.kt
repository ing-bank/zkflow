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

class SerializerProviderGenerator(private val codeGenerator: CodeGenerator) {

    fun generateProviders(serializableClasses: Map<SerializableClassWithSourceFiles, Int>): ServiceLoaderRegistration {
        val providerClasses = serializableClasses.map { (implementationClass, implementationId) ->
            generateProvider(implementationClass, implementationId)
        }
        return ServiceLoaderRegistration(KClassSerializerProvider::class, providerClasses)
    }

    private fun generateProvider(
        serializableClassWithSourceFiles: SerializableClassWithSourceFiles,
        implementationId: Int,
    ): String {
        val className = "${serializableClassWithSourceFiles.className.simpleNames.joinToString("") { it }}SerializerProvider"
        FileSpec.builder(serializableClassWithSourceFiles.className.packageName, className).addType(
            TypeSpec.classBuilder(className).addSuperinterface(KClassSerializerProvider::class).addFunction(
                FunSpec.builder("get").addModifiers(KModifier.OVERRIDE)
                    .returns(KClassSerializer::class.asClassName().parameterizedBy(STAR)).addCode(
                        buildCodeBlock {
                            addStatement(
                                "return %T(%L::class, %L, %L.serializer())",
                                KClassSerializer::class,
                                serializableClassWithSourceFiles.className,
                                implementationId,
                                serializableClassWithSourceFiles.className,
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
                aggregating = false, originatingKSFiles = serializableClassWithSourceFiles.sourceFiles
            )
        return "${serializableClassWithSourceFiles.className.packageName}.$className"
    }

    data class SerializableClassWithSourceFiles(
        val className: ClassName,
        val sourceFiles: List<KSFile> = emptyList()
    ) {
        constructor(className: ClassName, sourceFile: KSFile?) : this(className, listOfNotNull(sourceFile))

        companion object {
            fun KSClassDeclaration.toGeneratedSerializer(): SerializableClassWithSourceFiles =
                SerializableClassWithSourceFiles(toClassName(), containingFile)
        }
    }
}
