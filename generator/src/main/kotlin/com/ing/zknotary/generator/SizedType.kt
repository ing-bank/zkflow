package com.ing.zknotary.generator

import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zknotary.util.describe
import com.ing.zknotary.util.sizedName
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

            return FileSpec.builder(clazz.packageName.asString(), clazz.sizedName)
                .addType(
                    buildFixedLengthType(clazz.sizedName, clazz, annotatedClasses)
                )
                .build()
        }

        private fun buildFixedLengthType(
            generatedClassName: String,
            clazz: KSClassDeclaration,
            annotatedClasses: List<KSClassDeclaration>
        ) = TypeSpec.classBuilder(generatedClassName).apply {
            val original = "original"

            val privateConstructorBuilder = FunSpec.constructorBuilder()
                .addModifiers(KModifier.PRIVATE)

            val instanceConstructorBuilder = FunSpec.constructorBuilder()
                .addParameter(original, clazz.toTypeName())

            val defaultConstructor = FunSpec.constructorBuilder()

            val properties = clazz.getAllProperties()
                .filter { property -> property.isPublic() }
                .map { property ->
                    val descriptor = property.describe(original, annotatedClasses)

                    // Side effect: defining properties and private constructor.
                    privateConstructorBuilder.addParameter(descriptor.name, descriptor.type)
                    addProperty(
                        PropertySpec.builder(descriptor.name, descriptor.type)
                            .initializer(descriptor.name)
                            .build()
                    )

                    descriptor
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
