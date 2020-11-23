package com.ing.zknotary.generator

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
<<<<<<< HEAD
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.util.construct
import com.ing.zknotary.util.findAnnotationWithType
import com.ing.zknotary.util.toTypeName
import com.ing.zknotary.util.typeName
=======
import com.ing.zknotary.util.describe
import com.ing.zknotary.util.toTypeName
>>>>>>> f18adea... f
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class SizedType {
    companion object {
        fun fromClass(
            clazz: KSClassDeclaration,
            annotatedClasses: List<KSClassDeclaration>,
            logger: KSPLogger
        ): FileSpec {
            val generatedClassName = "${clazz.simpleName.asString()}Sized"

            return FileSpec.builder(clazz.packageName.asString(), generatedClassName)
                .addType(
                    buildFixedLengthType(generatedClassName, clazz, annotatedClasses, logger)
                )
                .addImport("com.ing.zknotary.annotations", "extend")
                .build()
        }

        private fun buildFixedLengthType(
            generatedClassName: String,
            clazz: KSClassDeclaration,
            annotatedClasses: List<KSClassDeclaration>,
            logger: KSPLogger
        ): TypeSpec {
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
                        val construction = property.construct(original, logger)

                        construction.debug(logger)

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