package com.ing.zkflow.processors.serialization

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.common.serialization.KClassSerializer
import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.getSurrogateSerializerClassName
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
                            val serializerClassName = when (serializableClassWithSourceFiles) {
                                is SerializableClassWithSourceFiles.Existing -> serializableClassWithSourceFiles.declaration.getSurrogateSerializerClassName()
                                is SerializableClassWithSourceFiles.Generated -> className
                            }

                            addStatement(
                                "return %T(%T::class, %L, %T)",
                                KClassSerializer::class,
                                serializableClassWithSourceFiles.className,
                                implementationId,
                                serializerClassName,
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

    sealed class SerializableClassWithSourceFiles(
        val sourceFiles: List<KSFile> = emptyList()
    ) {
        class Generated(override val className: ClassName, sourceFiles: List<KSFile>) : SerializableClassWithSourceFiles(sourceFiles)

        class Existing(val declaration: KSClassDeclaration, sourceFiles: List<KSFile>) : SerializableClassWithSourceFiles(sourceFiles) {
            override val className: ClassName
                get() = declaration.toClassName()
        }

        abstract val className: ClassName

        companion object {
            fun KSClassDeclaration.toGeneratedSerializer(): SerializableClassWithSourceFiles =
                Existing(this, listOfNotNull(containingFile))
        }
    }
}
