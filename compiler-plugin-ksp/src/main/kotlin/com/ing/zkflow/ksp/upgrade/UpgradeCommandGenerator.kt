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
import java.util.Locale

/**
 * Generates version upgrade commands for families of state or command classes.
 * Upgrades are generated in steps, one at a time.
 * For each upgrade a public (any) and a private upgrade command is generated. There are no private-to-public or
 * any-to-private upgrade commands, because the only goal here is to make sure that states are converted to the
 * right version.
 */
class UpgradeCommandGenerator(
    private val codeGenerator: CodeGenerator
) {
    @Suppress("LongMethod")
    fun process(families: Map<String, List<KSClassDeclaration>>): List<ClassName> {
        return families.entries.flatMap { (_, members) ->
            var previousVersion: KSClassDeclaration? = null
            members.flatMap { current ->
                previousVersion?.let { previous ->
                    listOf(
                        generateUpgradeCommand(previous, current, isPrivate = true),
                        generateUpgradeCommand(previous, current, isPrivate = false),
                    )
                }
                    .orEmpty()
                    .also { previousVersion = current }
            }
        }
    }

    private fun generateUpgradeCommand(
        previous: KSClassDeclaration,
        current: KSClassDeclaration,
        isPrivate: Boolean
    ): ClassName {
        val publicOrPrivateOutput = if (isPrivate) "private" else "public"
        val publicOrAnyInput = if (isPrivate) "private" else "any"
        val commandClassName =
            "Upgrade${publicOrAnyInput.capitalize(Locale.getDefault())}${previous.simpleName.asString()}To${publicOrPrivateOutput.capitalize(Locale.getDefault())}${current.simpleName.asString()}"
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
                                                $publicOrAnyInput(%1L::class) at 0
                                            }
                                            outputs {
                                                $publicOrPrivateOutput(%2L::class) at 0
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
        return ClassName(current.packageName.asString(), commandClassName)
    }
}
