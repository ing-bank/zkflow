package com.ing.zknotary.generator

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zknotary.util.toTypeName
import com.ing.zknotary.util.typeName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class FixedLengthType {
    companion object {
        fun fromClass(clazz: KSClassDeclaration): FileSpec {
            val generatedClassName = "Fixed${clazz.simpleName.asString()}"
            val file = FileSpec.builder(clazz.packageName.asString(), generatedClassName)
                .addType(buildFixedLengthType(generatedClassName, clazz))
                .build()
            return file
        }

        private fun buildFixedLengthType(
            generatedClassName: String,
            clazz: KSClassDeclaration
        ): TypeSpec {
            return TypeSpec.classBuilder(generatedClassName).apply {
                val primaryConstructorBuilder = FunSpec.constructorBuilder().addModifiers(KModifier.PRIVATE)
                val factoryConstructorBuilder = FunSpec.constructorBuilder()
                val propertyNames = mutableListOf<String>()

                clazz.getAllProperties()
                    .filter { property -> property.isPublic() }
                    .forEach { property ->
                        val name = property.simpleName.asString()
                        propertyNames.add(name)

                        val typeName = property.type.resolve().typeName

                        primaryConstructorBuilder.addParameter(name, typeName)

                        addProperty(
                            PropertySpec.builder(name, typeName)
                                .initializer(name)
                                .build()
                        )
                    }

                primaryConstructor(primaryConstructorBuilder.build())

                @Suppress("SpreadOperator")
                factoryConstructorBuilder
                    .addParameter("original", clazz.toTypeName())
                    .callThisConstructor(
                        CodeBlock.of(
                            propertyNames.map { "original.%N" }.joinToString(", "),
                            // "original.%N, original.%N, original.%N",
                            args = *propertyNames.toTypedArray()
                        )
                    )
                addFunction(factoryConstructorBuilder.build())
            }.build()
        }
    }
}