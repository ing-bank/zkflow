package com.ing.zknotary.generator

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
<<<<<<< HEAD
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.util.isList
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
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class SizedType {
    companion object {
        fun fromClass(clazz: KSClassDeclaration, logger: KSPLogger): FileSpec {
            val generatedClassName = "${clazz.simpleName.asString()}Sized"
            return FileSpec.builder(clazz.packageName.asString(), generatedClassName)
                .addType(buildFixedLengthType(generatedClassName, clazz, logger))
                .addImport("com.ing.zknotary.annotations", "extend")
                .build()
        }

        private fun buildFixedLengthType(
            generatedClassName: String,
            clazz: KSClassDeclaration,
            logger: KSPLogger
        ): TypeSpec {
            return TypeSpec.classBuilder(generatedClassName).apply {
                val privateConstructorBuilder = FunSpec.constructorBuilder().addModifiers(KModifier.PRIVATE)
                val instanceConstructorBuilder = FunSpec.constructorBuilder()
                val defaultConstructor = FunSpec.constructorBuilder()

                val properties = mutableListOf<Triple<String, String, String>>()

                clazz.getAllProperties()
                    .filter { property -> property.isPublic() }
                    .map { property ->
                        val construction = property.describe(original, logger)

                            Pair("List($size) { $default }", ".extend($size, 0)")
                        } else {
                            val default = 0

                            Pair("$default", "")
                        }

                        // Effect 1. Construction of metadata for default and instance constructors.
                        properties.add(Triple(name, init.first, init.second))

                        // Effect 2. Construction of the private constructor.
                        val typeName = type.resolve().typeName
                        privateConstructorBuilder.addParameter(name, typeName)
                        addProperty(
                            PropertySpec.builder(name, typeName)
                                .initializer(name)
                                .build()
                        )
                    }

                primaryConstructor(privateConstructorBuilder.build())

                @Suppress("SpreadOperator")
                instanceConstructorBuilder
                    .addParameter("original", clazz.toTypeName())
                    .callThisConstructor(
                        CodeBlock.of(
                            properties.map { it.third }.joinToString { "original.%N$it" },
                            args = *properties.map {it.first}.toTypedArray()
                        )
                    )
                addFunction(instanceConstructorBuilder.build())

                defaultConstructor.callThisConstructor(
                    CodeBlock.of(properties.joinToString { it.second })
                )
                addFunction(defaultConstructor.build())
            }.build()
        }
    }
}