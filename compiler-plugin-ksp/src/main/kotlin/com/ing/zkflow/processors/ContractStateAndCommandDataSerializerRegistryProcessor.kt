package com.ing.zkflow.processors

import com.google.devtools.ksp.processing.CodeGenerator
import com.ing.zkflow.common.serialization.ZKDataRegistryProvider
import com.ing.zkflow.ksp.implementations.ImplementationsProcessor
import com.ing.zkflow.ksp.implementations.ScopedDeclaration
import com.ing.zkflow.ksp.implementations.ServiceLoaderRegistration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.withIndent
import kotlinx.serialization.KSerializer
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.reflect.KClass

class ContractStateAndCommandDataSerializerRegistryProcessor<T : Any>(
    override val interfaceClass: KClass<T>,
    private val mapProviderInterface: KClass<out ZKDataRegistryProvider<in T>>,
    private val codeGenerator: CodeGenerator
) : ImplementationsProcessor<T> {

    override fun process(implementations: List<ScopedDeclaration>): ServiceLoaderRegistration {
        val uid = Random.nextInt().absoluteValue
        val packageName = "com.ing.zkflow.serialization.infra"
        val className = "${interfaceClass.simpleName}SerializerRegistryProvider$uid"

        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(mapProviderInterface)
                    .addFunction(
                        FunSpec.builder("list")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(
                                List::class.asClassName().parameterizedBy(
                                    Pair::class.asClassName().parameterizedBy(
                                        KClass::class.asClassName().parameterizedBy(
                                            WildcardTypeName.producerOf(interfaceClass)
                                        ),
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
                                        implementations.forEach { declaration ->
                                            addStatement(
                                                "%T(%L::class, %L.serializer()),",
                                                Pair::class,
                                                declaration.qualifiedName,
                                                declaration.qualifiedName
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

        return ServiceLoaderRegistration(mapProviderInterface, listOf("$packageName.$className"))
    }
}