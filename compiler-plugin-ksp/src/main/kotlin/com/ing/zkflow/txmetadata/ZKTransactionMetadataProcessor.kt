package com.ing.zkflow.txmetadata

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.ksp.ImplementationsVisitor
import com.ing.zkflow.ksp.ImplementationsVisitor.Companion.toMapOfLists
import com.ing.zkflow.ksp.MetaInfServicesBuilder
import com.ing.zkflow.ksp.ScopedDeclaration
import com.ing.zkflow.serialization.ZKContractStateSerializerMapProvider
import com.ing.zkflow.serialization.ZkCommandDataSerializerMapProvider
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
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.reflect.KClass

class ZKTransactionMetadataProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val zkCommandMetaInfServicesBuilder: MetaInfServicesBuilder = MetaInfServicesBuilder(
        environment.codeGenerator,
        ZKTransactionMetadataCommandData::class
    )
    private val zkContractStateMapMetaInfServicesBuilder: MetaInfServicesBuilder = MetaInfServicesBuilder(
        environment.codeGenerator,
        ZKContractStateSerializerMapProvider::class
    )
    private val zkCommandDataMapMetaInfServicesBuilder: MetaInfServicesBuilder = MetaInfServicesBuilder(
        environment.codeGenerator,
        ZkCommandDataSerializerMapProvider::class
    )
    private val implementationsVisitor = ImplementationsVisitor(
        listOf(
            ZKOwnableState::class,
            ZKTransactionMetadataCommandData::class,
            ZKContractStateSerializerMapProvider::class,
            ZkCommandDataSerializerMapProvider::class
        )
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        println("Handling: ${resolver.getNewFiles().joinToString { it.toString() }}")
        val implementations = resolver.getNewFiles()
            .flatMap { implementationsVisitor.visitFile(it, null) }
            .toList()
            .toMapOfLists()

        implementations.forEach { (interfaceClass, implementations) ->
            when (interfaceClass) {
                ZKOwnableState::class -> {
                    createProviderOf<ContractState>(implementations)
                }
                ZKTransactionMetadataCommandData::class -> {
                    createProviderOf<CommandData>(implementations)
                    zkCommandMetaInfServicesBuilder.createOrUpdate(implementations)
                }
                ZKContractStateSerializerMapProvider::class -> {
                    zkContractStateMapMetaInfServicesBuilder.createOrUpdate(implementations)
                }
                ZkCommandDataSerializerMapProvider::class -> {
                    zkCommandDataMapMetaInfServicesBuilder.createOrUpdate(implementations)
                }
                else -> throw IllegalArgumentException("Unexpected interface $interfaceClass")
            }
        }

        return emptyList()
    }

    private inline fun <reified T> createProviderOf(declarations: List<ScopedDeclaration>) {
        val uid = Random.nextInt().absoluteValue
        val packageName = "com.ing.zkflow.serialization"
        val className = "${T::class.simpleName}SerializerMapProvider$uid"
        val superinterface = when (T::class) {
            ContractState::class -> ZKContractStateSerializerMapProvider::class
            CommandData::class -> ZkCommandDataSerializerMapProvider::class
            else -> error("Indexes of only either `${ContractState::class.qualifiedName}` or `${CommandData::class.qualifiedName}` can be built")
        }

        FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addSuperinterface(superinterface)
                    .addFunction(
                        FunSpec.builder("list")
                            .addModifiers(KModifier.OVERRIDE)
                            // Build type List<Pair<KClass<out T>, KSerializer<out T>>>
                            .returns(
                                List::class.asClassName().parameterizedBy(
                                    Pair::class.asClassName().parameterizedBy(
                                        KClass::class.asClassName().parameterizedBy(
                                            WildcardTypeName.producerOf(T::class)
                                        ),
                                        KSerializer::class.asClassName().parameterizedBy(
                                            WildcardTypeName.producerOf(T::class)
                                        )
                                    )
                                )
                            )
                            .addCode(
                                buildCodeBlock {
                                    add("return listOf(")
                                    withIndent {
                                        declarations.forEach { declaration ->
                                            addStatement("%T(%L::class, %L.serializer()),", Pair::class, declaration.qualifiedName, declaration.qualifiedName)
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
            .writeTo(codeGenerator = environment.codeGenerator, aggregating = false)
    }
}
