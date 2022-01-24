package com.ing.zkflow.txmetadata

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.ksp.KotlinSymbolProcessor
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
import net.corda.core.internal.packageName
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.reflect.KClass

class ZKTransactionMetadataProcessor(
    private val environment: SymbolProcessorEnvironment
) : KotlinSymbolProcessor {
    override fun process(resolver: Resolver, allKotlinFiles: Sequence<KSFile>): List<KSAnnotated> {
        val zkPrimitivesIndex = allKotlinFiles
            .flatMap(this::indexZKPrimitives)
            .toList()

        zkPrimitivesIndex
            .filterIsInstance<ZKPrimitive.State>()
            .map { it.declaration }
            .ifNotEmpty {
                val providerClassName = createProviderOf<ContractState>(it)
                registerProvider<ZKContractStateSerializerMapProvider>(providerClassName)
            }

        zkPrimitivesIndex
            .filterIsInstance<ZKPrimitive.Command>()
            .map { it.declaration }
            .ifNotEmpty {
                val providerClassName = createProviderOf<CommandData>(it)
                registerProvider<ZkCommandDataSerializerMapProvider>(providerClassName)

                createMetaInfServicesFile(it)
            }

        return emptyList()
    }

    private fun indexZKPrimitives(ksFile: KSFile): List<ZKPrimitive> =
        ZKPrimitivesVisitor().visitFile(ksFile, null)

    @Suppress("SpreadOperator")
    private fun createMetaInfServicesFile(correctlyAnnotatedTransactions: List<ScopedDeclaration>) =
        environment.codeGenerator.createNewFile(
            Dependencies(
                false,
                *correctlyAnnotatedTransactions.mapNotNull { it.declaration.containingFile }.toList().toTypedArray()
            ),
            "META-INF/services",
            ZKTransactionMetadataCommandData::class.packageName,
            ZKTransactionMetadataCommandData::class.simpleName!!
        ).appendText(
            correctlyAnnotatedTransactions.joinToString("\n") { "${it.java.qualifiedName}\n" }
        )

    private inline fun <reified T> createProviderOf(declarations: List<ScopedDeclaration>): String {
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

        return "$packageName.$className"
    }

    private inline fun <reified T> registerProvider(providerClassName: String) =
        environment.codeGenerator.createNewFile(
            Dependencies(false),
            "META-INF/services",
            T::class.packageName, T::class.simpleName!!
        ).appendText(providerClassName)
}

private fun OutputStream.appendText(text: String) = use {
    write(text.toByteArray(StandardCharsets.UTF_8))
}

private fun <T> List<T>.ifNotEmpty(process: (it: List<T>) -> Unit) {
    if (isNotEmpty()) {
        process(this)
    }
}
