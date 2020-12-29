package com.ing.zknotary.generator

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zknotary.descriptors.describe
import com.ing.zknotary.util.sizedName
import com.ing.zknotary.util.toTypeName
import com.squareup.kotlinpoet.ClassName
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

            return FileSpec.builder(clazz.packageName.asString(), clazz.sizedName).apply {
                val (imports, type) = buildFixedLengthType(clazz, annotatedClasses)

                imports.forEach { addImport(it.packageName, it.simpleNames) }
                addType(type)
            }.build()
        }

        private fun buildFixedLengthType(
            clazz: KSClassDeclaration,
            annotatedClasses: List<KSClassDeclaration>
        ): Pair<List<ClassName>, TypeSpec> {
            val descriptors = clazz.getAllProperties()
                .filter { it.isPublic() }
                .map { it.describe(annotatedClasses) }

            val type = TypeSpec.classBuilder(clazz.sizedName).apply {
                // Define properties.
                descriptors.forEach {
                    addProperty(
                        PropertySpec.builder(it.name, it.typeDescriptor.type)
                            .initializer(it.name)
                            .build()
                    )
                }

                // Define constructors.
                val privateConstructor = FunSpec.constructorBuilder().apply {
                    addModifiers(KModifier.PRIVATE)
                    descriptors.forEach {
                        addParameter(it.name, it.typeDescriptor.type)
                    }
                }.build()
                primaryConstructor(privateConstructor)

                val original = "original"
                val instanceConstructor = FunSpec.constructorBuilder()
                    .addParameter(original, clazz.toTypeName())
                    .callThisConstructor(
                        CodeBlock.of(descriptors.joinToString { it.typeDescriptor.toCodeBlock("$original.${it.name}").toString() })
                    )
                    .build()
                addFunction(instanceConstructor)

                val defaultConstructor = FunSpec.constructorBuilder()
                    .callThisConstructor(
                        CodeBlock.of(descriptors.joinToString { it.typeDescriptor.default.toString() })
                    )
                addFunction(defaultConstructor.build())
            }.build()

            return Pair(
                descriptors.fold(listOf()) { acc, it -> acc + it.typeDescriptor.imports() },
                type
            )
        }
    }
}
