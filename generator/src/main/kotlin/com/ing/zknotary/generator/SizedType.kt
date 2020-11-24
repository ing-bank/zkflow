package com.ing.zknotary.generator

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zknotary.util.describe
import com.ing.zknotary.util.toTypeName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

var log: KSPLogger? = null

class SizedType {
    companion object {
        fun fromClass(
            clazz: KSClassDeclaration,
            annotatedClasses: List<KSClassDeclaration>,
            logger: KSPLogger
        ): FileSpec {
            log = logger

            val generatedClassName = "${clazz.simpleName.asString()}Sized"

            return FileSpec.builder(clazz.packageName.asString(), generatedClassName)
                .addType(
                    buildFixedLengthType(generatedClassName, clazz, annotatedClasses, logger)
                )
                .build()
        }

        private fun buildFixedLengthType(
            generatedClassName: String,
            clazz: KSClassDeclaration,
            annotatedClasses: List<KSClassDeclaration>,
            logger: KSPLogger
        ): TypeSpec {
            // val
            // logger.error("${WrappedList::class.java.`package`.name}, ${WrappedList::class.simpleName!!}")

            return TypeSpec.classBuilder(generatedClassName).apply {
                val original = "original"

                val privateConstructorBuilder = FunSpec.constructorBuilder()
                    .addModifiers(KModifier.PRIVATE)

                val instanceConstructorBuilder = FunSpec.constructorBuilder()
                        .addParameter(original, clazz.toTypeName())

                val defaultConstructor = FunSpec.constructorBuilder()

                val properties = clazz.getAllProperties()
                    .filter { property -> property.isPublic() }
                    .map { property ->
                        val construction = property.describe(original, logger)

                        // construction.debug(logger)

                        // Side effect: defining properties and private constructor.
                        privateConstructorBuilder.addParameter(construction.name, construction.type)
                        addProperty(
                            PropertySpec.builder(construction.name, construction.type)
                                .initializer(construction.name)
                                .build()
                        )

                        construction
                    }

                primaryConstructor(privateConstructorBuilder.build())

                instanceConstructorBuilder
                    .callThisConstructor(
                        CodeBlock.of(
                            properties.joinToString { it.fromInstance.toString() }
                        )
                    )
                addFunction(instanceConstructorBuilder.build())

                defaultConstructor.callThisConstructor(
                    CodeBlock.of(properties.joinToString { it.default.toString() })
                )
                addFunction(defaultConstructor.build())

            }.build()
        }
    }
}