package com.ing.zkflow.compilation

import com.ing.zkflow.common.zkp.metadata.CommandMetadataCache
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.CIRCUIT_SOURCES_STATES_PATH
import com.ing.zkflow.compilation.zinc.template.TemplateConfigurations
import com.ing.zkflow.compilation.zinc.template.TemplateRenderer
import com.ing.zkflow.compilation.zinc.template.parameters.SerializedStateTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.SignersTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.StateGroupTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.TxStateTemplateParameters
import com.ing.zkflow.compilation.zinc.util.CodeGenerator
import com.ing.zkflow.compilation.zinc.util.MerkleReplacer
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.File

@Suppress("LongMethod", "MagicNumber") // Just fixing it now, will be thrown away anyway soon for ZincPoet
@SuppressFBWarnings("PATH_TRAVERSAL_IN", justification = "WONTFIX: will be thrown away anyway soon for ZincPoet")
fun main(args: Array<String>) {
    val platformSourcesPath = File(args[0])
    val platformTemplatesPath = File(args[1])
    val circuitSourcesBase = File(args[2])
    val mergedCircuitOutput = File(args[3])
    val zkFlowTemplateConfigurationClass = args[4]

    circuitSourcesBase
        .listFiles { dir, file ->
            dir.resolve(file).isDirectory && file != CIRCUIT_SOURCES_STATES_PATH
        }
        ?.map { it.name }
        ?.forEach { circuitName ->
            val metadata = CommandMetadataCache.findCommandMetadata(circuitName)
            val circuitSourceOutputPath = mergedCircuitOutput.resolve(circuitName).resolve("src")

            val codeGenerator = CodeGenerator(circuitSourceOutputPath, metadata)

            // Generate consts file
            codeGenerator.generateConstsFile()

            val consts = joinConstFiles(circuitSourceOutputPath, platformSourcesPath)

            // Render templates
            val templateRenderer = TemplateRenderer(circuitSourceOutputPath.toPath()) { params ->
                getTemplateContents(platformTemplatesPath, params.templateFile)
            }

            val templateConfigurationsForCircuit = getTemplateConfiguration(zkFlowTemplateConfigurationClass)

            templateConfigurationsForCircuit.apply {
                metadata.circuit.javaClass2ZincType.forEach { (_, zincType) ->
                    addConfigurations(TxStateTemplateParameters(metadata, zincType))
                }

                addConfigurations(SignersTemplateParameters(metadata))
            }
                .resolveAllTemplateParameters()
                .forEach(templateRenderer::renderTemplate)

            // Render multi-state templates
            renderStateTemplates(metadata, templateRenderer, templateConfigurationsForCircuit)

            // Generate code
            getTemplateContents(platformTemplatesPath, "merkle_template.zn").also { codeGenerator.generateMerkleUtilsCode(it, consts) }
            getTemplateContents(platformTemplatesPath, "main_template.zn").also { codeGenerator.generateMainCode(it, consts) }

            // Replace placeholders in Merkle tree functions
            val replacer = MerkleReplacer(circuitSourceOutputPath)
            replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
            replacer.setCorrespondingMerkleTreeFunctionForMainTree()
        }
}

private fun getTemplateContents(templateRootPath: File, templateName: String) =
    runCatching {
        templateRootPath.listFiles() ?: error("Templates must be accessible")
    }
        .mapCatching { templates ->
            templates.single { it.name == templateName } ?: error("Multiple templates for $templateName found")
        }
        .map { it.readText() }
        .getOrThrow()

fun renderStateTemplates(
    metadata: ResolvedZKCommandMetadata,
    templateRenderer: TemplateRenderer,
    templateConfigurationsForCircuit: TemplateConfigurations
) {
    templateConfigurationsForCircuit.apply {
        metadata.privateInputTypeGroups.forEach { (type, count) ->
            addConfigurations(
                SerializedStateTemplateParameters(
                    "input",
                    count,
                    metadata.circuit.javaClass2ZincType[type]
                        ?: error("No Zinc Type defined for $type")
                )
            )
        }
        addConfigurations(StateGroupTemplateParameters("input", metadata.privateInputTypeGroups, metadata.circuit.javaClass2ZincType))

        metadata.privateOutputTypeGroups.forEach { (type, count) ->
            addConfigurations(
                SerializedStateTemplateParameters(
                    "output",
                    count,
                    metadata.circuit.javaClass2ZincType[type]
                        ?: error("No Zinc Type defined for $type")
                )
            )
        }
        addConfigurations(StateGroupTemplateParameters("output", metadata.privateOutputTypeGroups, metadata.circuit.javaClass2ZincType))

        metadata.privateReferenceTypeGroups.forEach { (type, count) ->
            addConfigurations(
                SerializedStateTemplateParameters(
                    "reference",
                    count,
                    metadata.circuit.javaClass2ZincType[type]
                        ?: error("No Zinc Type defined for $type")
                )
            )
        }
        addConfigurations(StateGroupTemplateParameters("reference", metadata.privateReferenceTypeGroups, metadata.circuit.javaClass2ZincType))
    }.resolveAllTemplateParameters()
        .forEach(templateRenderer::renderTemplate)
}

private fun getTemplateConfiguration(zkFlowTemplateConfigurationClass: String): TemplateConfigurations {
    return try {
        Class.forName(zkFlowTemplateConfigurationClass)
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("Could not find object '$zkFlowTemplateConfigurationClass' on the classpath", e)
    }.kotlin.objectInstance as? TemplateConfigurations
        ?: error("'$zkFlowTemplateConfigurationClass' should be a singleton object")
}
