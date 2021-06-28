package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.plugin.circuitNames
import com.ing.zknotary.gradle.plugin.getTemplateContents
import com.ing.zknotary.gradle.plugin.platformSourcesRootPath
import com.ing.zknotary.gradle.plugin.zkNotaryExtension
import com.ing.zknotary.gradle.zinc.template.TemplateRenderer
import com.ing.zknotary.gradle.zinc.template.parameters.SignersTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.TxStateTemplateParameters
import com.ing.zknotary.gradle.zinc.util.CircuitConfigurator
import com.ing.zknotary.gradle.zinc.util.CodeGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        val extension = project.zkNotaryExtension

        project.circuitNames?.forEach { circuitName ->
            val circuitSources = extension.circuitSourcesBasePath.resolve(circuitName)
            val configurator = CircuitConfigurator(circuitSources, ZKNotaryExtension.CONFIG_CIRCUIT_FILE)

            val circuitSourceOutputPath = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src")

            // Generate consts file
            configurator.generateConstsFile(circuitSourceOutputPath)

            val codeGenerator = CodeGenerator(circuitSourceOutputPath)
            val consts = joinConstFiles(circuitSourceOutputPath, project.platformSourcesRootPath)

            codeGenerator.generateMerkleUtilsCode(project.getTemplateContents(extension.merkleTemplate), consts)
            codeGenerator.generateMainCode(project.getTemplateContents(extension.mainTemplate), consts)

            val templateRenderer = TemplateRenderer(circuitSourceOutputPath.toPath()) { params ->
                project.getTemplateContents(params.templateFile)
            }

            extension
                .apply {
                    configurator.circuitConfiguration.circuit.states.forEach { state ->
                        addConfigurations(TxStateTemplateParameters(state))
                    }
                    addConfigurations(SignersTemplateParameters(configurator.circuitConfiguration.groups.signerGroup))
                }.resolveAllTemplateParameters()
                .forEach(templateRenderer::renderTemplate)
        }
    }
}
