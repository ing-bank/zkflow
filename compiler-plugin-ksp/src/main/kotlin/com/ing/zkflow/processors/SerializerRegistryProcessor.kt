package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zkflow.common.serialization.KClassSerializerProvider
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.withIndent
import kotlinx.serialization.KSerializer
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.reflect.KClass

class SerializerRegistryProcessor<T : Any>(
    private val interfaceClass: KClass<T>,
    private val mapProviderInterface: KClass<out KClassSerializerProvider<in T>>,
    private val codeGenerator: CodeGenerator
) {
    private val packageName = "com.ing.zkflow.serialization.infra"

    fun process(implementations: Map<KSClassDeclaration, Int>): ServiceLoaderRegistration {
        val uid = Random.nextInt().absoluteValue
        val className = "${interfaceClass.simpleName}SerializerRegistryProvider$uid"

        generateProvider(className, implementations)

        return ServiceLoaderRegistration(mapProviderInterface, listOf("$packageName.$className"))
    }

    private fun generateProvider(
        className: String,
        implementations: Map<KSClassDeclaration, Int>
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
                                    Triple::class.asClassName().parameterizedBy(
                                        KClass::class.asClassName().parameterizedBy(
                                            WildcardTypeName.producerOf(interfaceClass)
                                        ),
                                        Int::class.asClassName(),
                                        KSerializer::class.asClassName().parameterizedBy(
                                            WildcardTypeName.producerOf(interfaceClass)
                                        )
                                    )
                                )
                            )
                            .addCode(
                                buildCodeBlock {
                                    add("return listOf(")
                                    withIndent {
                                        implementations.entries.forEach { (impl, version) ->
                                            addStatement(
                                                "%T(%L::class, %L, %L.serializer()),",
                                                Triple::class,
                                                impl.toClassName(),
                                                version,
                                                impl.toClassName(),
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
