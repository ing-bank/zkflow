package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.ing.zkflow.common.serialization.KClassSerializer
import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.withIndent
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.reflect.KClass

class SerializerRegistryProcessor<T : Any>(
    private val interfaceClass: KClass<T>,
    private val mapProviderInterface: KClass<out KClassSerializerProvider<in T>>,
    private val codeGenerator: CodeGenerator
) {
    private val packageName = "com.ing.zkflow.serialization.infra"

    fun process(implementations: Map<ClassName, Int>): ServiceLoaderRegistration {
        val uid = Random.nextInt().absoluteValue
        val className = "${interfaceClass.simpleName}SerializerRegistryProvider$uid"

        generateProvider(className, implementations)

        return ServiceLoaderRegistration(mapProviderInterface, listOf("$packageName.$className"))
    }

    private fun generateProvider(
        className: String,
        implementations: Map<ClassName, Int>
    ) {
        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(mapProviderInterface)
                    .addFunction(
                        FunSpec.builder("list")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(
                                List::class.asClassName().parameterizedBy(
                                    KClassSerializer::class.asClassName().parameterizedBy(
                                        interfaceClass.asClassName()
                                    )
                                )
                            )
                            .addCode(
                                buildCodeBlock {
                                    add("return listOf(")
                                    withIndent {
                                        implementations.entries.forEach { (implClass, version) ->
                                            addStatement(
                                                "%T(%L::class, %L, %L.serializer()),",
                                                KClassSerializer::class,
                                                implClass,
                                                version,
                                                implClass,
                                            )
                                        }
                                    }
                                    add(")")
                                }
                            )
                            .build()
                    )
                    .build()
            )
            .build()
            .writeTo(codeGenerator = codeGenerator, aggregating = false)
    }
}
