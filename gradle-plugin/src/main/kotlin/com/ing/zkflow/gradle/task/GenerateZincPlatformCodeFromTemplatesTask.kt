package com.ing.zkflow.gradle.task

import com.ing.zkflow.common.zkp.metadata.TransactionMetadataCache.findMetadataByCircuitName
import com.ing.zkflow.compilation.joinConstFiles
import com.ing.zkflow.compilation.zinc.template.TemplateRenderer
import com.ing.zkflow.compilation.zinc.template.parameters.SerializedStateTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.SignersTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.StateGroupTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.TxStateTemplateParameters
import com.ing.zkflow.compilation.zinc.util.CodeGenerator
import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.getTemplateContents
import com.ing.zkflow.gradle.plugin.platformSourcesRootPath
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @Suppress("NestedBlockDepth")
    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        val extension = project.zkFlowExtension

        project.circuitNames?.forEach { circuitName ->
            val metadata = findMetadataByCircuitName(circuitName)
            val circuitSourceOutputPath = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src")

            val codeGenerator = CodeGenerator(circuitSourceOutputPath, metadata)

            // Generate consts file
            codeGenerator.generateConstsFile()

            val consts = joinConstFiles(circuitSourceOutputPath, project.platformSourcesRootPath)

            codeGenerator.generateMerkleUtilsCode(project.getTemplateContents(extension.merkleTemplate), consts)
            codeGenerator.generateMainCode(project.getTemplateContents(extension.mainTemplate), consts)

            val templateRenderer = TemplateRenderer(circuitSourceOutputPath.toPath()) { params ->
                project.getTemplateContents(params.templateFile)
            }

            extension
                .apply {
                    metadata.javaClass2ZincType.forEach { (_, zincType) ->
                        addConfigurations(TxStateTemplateParameters(metadata, zincType))
                    }

                    addConfigurations(SignersTemplateParameters(metadata))

                    metadata.inputTypeGroups.filter { it.count > 0 }.forEach { contractStateTypeCount ->
                        addConfigurations(
                            SerializedStateTemplateParameters(
                                "input",
                                contractStateTypeCount,
                                metadata.javaClass2ZincType[contractStateTypeCount.type]
                                    ?: error("No Zinc Type defined for ${contractStateTypeCount.type}")
                            )
                        )
                    }
                    addConfigurations(StateGroupTemplateParameters("input", metadata.inputTypeGroups, metadata.javaClass2ZincType))

                    metadata.outputTypeGroups.filter { it.count > 0 }.forEach { contractStateTypeCount ->
                        addConfigurations(
                            SerializedStateTemplateParameters(
                                "output",
                                contractStateTypeCount,
                                metadata.javaClass2ZincType[contractStateTypeCount.type]
                                    ?: error("No Zinc Type defined for ${contractStateTypeCount.type}")
                            )
                        )
                    }
                    addConfigurations(StateGroupTemplateParameters("output", metadata.outputTypeGroups, metadata.javaClass2ZincType))

                    metadata.referenceTypeGroups.filter { it.count > 0 }.forEach { contractStateTypeCount ->
                        addConfigurations(
                            SerializedStateTemplateParameters(
                                "reference",
                                contractStateTypeCount,
                                metadata.javaClass2ZincType[contractStateTypeCount.type]
                                    ?: error("No Zinc Type defined for ${contractStateTypeCount.type}")
                            )
                        )
                    }
                    addConfigurations(StateGroupTemplateParameters("reference", metadata.referenceTypeGroups, metadata.javaClass2ZincType))
                }.resolveAllTemplateParameters()
                .forEach(templateRenderer::renderTemplate)
        }
    }
}
