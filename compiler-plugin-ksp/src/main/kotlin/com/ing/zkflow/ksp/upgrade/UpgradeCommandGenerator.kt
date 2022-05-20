package com.ing.zkflow.ksp.upgrade

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKUpgradeCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import net.corda.core.internal.writeText
import java.nio.file.Files

class UpgradeCommandGenerator(
    private val codeGenerator: CodeGenerator
) {
    @Suppress("LongMethod")
    fun process(families: Map<String, List<KSClassDeclaration>>): List<ClassName> {
        Files.createTempFile("families-", "-cosy").writeText(
            families.entries.joinToString("\n") { (familyName, members) ->
                "$familyName : ${members.joinToString { it.qualifiedName?.asString() ?: "${it.packageName.asString()}.${it.simpleName.asString()}" }}"
            }
        )

        return families.entries.flatMap { (_, members) ->
            var previousVersion: KSClassDeclaration? = null
            members.mapNotNull { current ->
                val generatedUpgradeCommand = previousVersion?.let { previous ->
                    val commandClassName = "Upgrade${previous.simpleName.asString()}To${current.simpleName.asString()}"
                    val qualifiedClassName = ClassName(current.packageName.asString(), commandClassName)
                    FileSpec.builder(current.packageName.asString(), commandClassName)
                        .addImport("com.ing.zkflow.common.zkp.metadata", "commandMetadata")
                        .addType(
                            TypeSpec.classBuilder(commandClassName)
                                .addAnnotation(ZKP::class)
                                .addSuperinterface(ZKUpgradeCommandData::class)
                                .addProperty(
                                    PropertySpec.builder("metadata", ResolvedZKCommandMetadata::class, KModifier.OVERRIDE)
                                        .mutable(false)
                                        .initializer(
                                            CodeBlock.of(
                                                """
                                                    commandMetadata {
                                                        circuit {
                                                            name = "%3L"
                                                        }
                                                        numberOfSigners = 1
                                                        command = true
                                                        notary = true
                                                        inputs {
                                                            private(%1L::class) at 0
                                                        }
                                                        outputs {
                                                            private(%2L::class) at 0
                                                        }
                                                    }                                                    
                                                """.trimIndent(),
                                                previous.qualifiedName?.asString(),
                                                current.qualifiedName?.asString(),
                                                commandClassName.camelToSnakeCase()
                                            )
                                        )
                                        .build()
                                )
                                .addFunction(
                                    FunSpec.builder("verifyPrivate")
                                        .addModifiers(KModifier.OVERRIDE)
                                        .returns(String::class)
                                        .addCode(
                                            CodeBlock.of(
                                                "return com.ing.zkflow.zinc.poet.generate.generateUpgradeVerification(metadata).generate()"
                                            )
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                        .writeTo(codeGenerator = codeGenerator, aggregating = false)
                    qualifiedClassName
                }
                previousVersion = current
                generatedUpgradeCommand
            }
        }
    }
}
