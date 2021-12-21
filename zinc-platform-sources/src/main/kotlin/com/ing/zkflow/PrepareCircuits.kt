@file:Suppress("MagicNumber") // Magic numbers will be removed when generating zinc based on Kotlin size annotations
package com.ing.zkflow

import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.common.zkp.metadata.TransactionMetadataCache
import com.ing.zkflow.compilation.joinConstFiles
import com.ing.zkflow.compilation.renderStateTemplates
import com.ing.zkflow.compilation.zinc.template.TemplateConfigurations
import com.ing.zkflow.compilation.zinc.template.TemplateConfigurations.Companion.doubleTemplateParameters
import com.ing.zkflow.compilation.zinc.template.TemplateConfigurations.Companion.floatTemplateParameters
import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.compilation.zinc.template.TemplateRenderer
import com.ing.zkflow.compilation.zinc.template.parameters.AbstractPartyTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.AbstractPartyTemplateParameters.Companion.ANONYMOUS_PARTY_TYPE_NAME
import com.ing.zkflow.compilation.zinc.template.parameters.AmountTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.BigDecimalTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.CollectionTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.IntegerTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.IssuedTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.MapTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.PublicKeyTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.SignersTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.StringTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.TxStateTemplateParameters
import com.ing.zkflow.compilation.zinc.util.CodeGenerator
import com.ing.zkflow.compilation.zinc.util.MerkleReplacer
import com.ing.zkflow.compilation.zinc.util.ZincSourcesCopier
import com.ing.zkflow.contract.TestMultipleStateContract
import com.ing.zkflow.testing.fixtures.contract.TestContract
import net.corda.core.crypto.Crypto
import java.io.File

val templateConfigurations = getTemplateConfiguration()

@Suppress("LongMethod") // Just fixing it now, will be thrown away anyway soon
fun main(args: Array<String>) {
    val root = args[0]
    val projectVersion = args[1]

    // Render templates for circuits testing deserialization, etc.
    TemplateRenderer(getPlatformSourcesTestSourcesPath(root).toPath()) {
        getTemplateContents(root, it.templateFile)
    }.apply {
        templateConfigurations.resolveAllTemplateParameters().forEach(::renderTemplate)
    }

    val circuitSourcesBase = File("$root/circuits")
    val statesPath = "states"
    val circuitStatesPath = circuitSourcesBase.resolve(statesPath)
    val mergedCircuitOutput = File("$root/build/circuits")

    circuitSourcesBase
        .listFiles { dir, file ->
            dir.resolve(file).isDirectory && file != statesPath
        }
        ?.map { it.name }
        ?.forEach { circuitName ->
            val outputPath = mergedCircuitOutput.resolve(circuitName).resolve("src")
            val circuitSourcesPath = circuitSourcesBase.resolve(circuitName)

            // Required to initialize the metadata cache
            TestContract.Create().transactionMetadata
            TestContract.Move().transactionMetadata
            TestMultipleStateContract.Move().transactionMetadata

            val metadata = TransactionMetadataCache.findMetadataByCircuitName(circuitName)

            val codeGenerator = CodeGenerator(outputPath, metadata)
            codeGenerator.generateConstsFile()

            // Copy Zinc sources
            val copier = ZincSourcesCopier(outputPath)
            copier.copyZincCircuitSources(
                circuitSourcesPath,
                circuitName,
                projectVersion
            )

            copier.copyZincCircuitStates(getCircuitStates(circuitStatesPath, metadata))
            copier.copyZincPlatformSources(getPlatformSources(root))
            copier.copyZincPlatformSources(getPlatformLibs(root))

            val consts = joinConstFiles(outputPath, getPlatformSourcesPath(root))

            // Render templates
            val templateRenderer = TemplateRenderer(outputPath.toPath()) {
                getTemplateContents(root, it.templateFile)
            }

            val templateConfigurationsForCircuit = getTemplateConfiguration()

            templateConfigurationsForCircuit.apply {
                metadata.javaClass2ZincType.forEach { (_, zincType) ->
                    addConfigurations(TxStateTemplateParameters(metadata, zincType))
                }

                addConfigurations(SignersTemplateParameters(metadata))
            }
                .resolveAllTemplateParameters()
                .forEach(templateRenderer::renderTemplate)

            // Render multi-state templates
            renderStateTemplates(metadata, templateRenderer, templateConfigurationsForCircuit)

            // Generate code
            getTemplateContents(root, "merkle_template.zn").also { codeGenerator.generateMerkleUtilsCode(it, consts) }
            getTemplateContents(root, "main_template.zn").also { codeGenerator.generateMainCode(it, consts) }

            // Replace placeholders in Merkle tree functions
            val replacer = MerkleReplacer(outputPath)
            replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
            replacer.setCorrespondingMerkleTreeFunctionForMainTree()
        }
}

private fun getPlatformSourcesPath(root: String): File {
    return File("$root/src/main/resources/zinc-platform-sources")
}

private fun getCircuitStates(circuitStatesPath: File, metadata: ResolvedZKTransactionMetadata): List<File> {
    return metadata.javaClass2ZincType.map { (_, zincType) ->
        val module = circuitStatesPath.resolve(zincType.fileName)
        require(module.exists()) { "Expected ${module.absolutePath}" }
        module
    }
}

private fun getPlatformSources(root: String): Array<File>? {
    return File("$root/src/main/resources/zinc-platform-sources").listFiles()
}

private fun getPlatformLibs(root: String): Array<File>? {
    return File("$root/src/main/resources/zinc-platform-libraries").listFiles()
}

private fun getPlatformSourcesTestSourcesPath(root: String): File {
    return File("$root/build/zinc-platform-test-sources")
}

private fun getTemplateContents(root: String, templateName: String) =
    runCatching {
        File("$root/src/main/resources/zinc-platform-templates").listFiles() ?: error("Templates must be accessible")
    }
        .mapCatching { templates ->
            templates.single { it.name == templateName } ?: error("Multiple templates for $templateName found")
        }
        .map { it.readText() }
        .getOrThrow()

private fun getTemplateConfiguration(): TemplateConfigurations {
    return TemplateConfigurations().apply {
        // BigDecimal configurations
        val bigDecimalTemplateParameters = listOf(
            BigDecimalTemplateParameters(24, 6),
            BigDecimalTemplateParameters(100, 20),
            floatTemplateParameters,
            doubleTemplateParameters,
        )
        addConfigurations(bigDecimalTemplateParameters)

        // Amount configurations
        val amountTemplateParameters = bigDecimalTemplateParameters.map { AmountTemplateParameters(it, 8) }
        addConfigurations(amountTemplateParameters)

        // String configurations
        addConfigurations(StringTemplateParameters(32))

        // Issued configurations
        addConfigurations(
            IssuedTemplateParameters(
                AbstractPartyTemplateParameters.selectAbstractPartyParameters(Crypto.EDDSA_ED25519_SHA512.schemeCodeName),
                StringTemplateParameters(1)
            )
        )

        addConfigurations(
            CollectionTemplateParameters(collectionSize = 3, innerTemplateParameters = StringTemplateParameters(1)),
            CollectionTemplateParameters<TemplateParameters>(
                "collection_integer.zn",
                collectionSize = 3,
                platformModuleName = "u32"
            ),
            CollectionTemplateParameters<TemplateParameters>(
                "collection_integer.zn",
                collectionSize = 2,
                platformModuleName = "i32"
            )
        )

        // Collection of participants to TestState.
        addConfigurations(
            CollectionTemplateParameters(
                collectionSize = 1,
                innerTemplateParameters = AbstractPartyTemplateParameters(
                    ANONYMOUS_PARTY_TYPE_NAME,
                    PublicKeyTemplateParameters.eddsaTemplateParameters
                )
            ),
            CollectionTemplateParameters(
                collectionSize = 2,
                innerTemplateParameters = AbstractPartyTemplateParameters(
                    ANONYMOUS_PARTY_TYPE_NAME,
                    PublicKeyTemplateParameters.eddsaTemplateParameters
                )
            )
        )

        addConfigurations(
            MapTemplateParameters(
                "StringToIntMap",
                6,
                StringTemplateParameters(5),
                IntegerTemplateParameters.i32
            )
        )
    }
}
