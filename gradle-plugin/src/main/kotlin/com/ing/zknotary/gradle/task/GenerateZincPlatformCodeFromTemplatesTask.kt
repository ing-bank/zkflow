package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.plugin.circuitNames
import com.ing.zknotary.gradle.plugin.getTemplateContents
import com.ing.zknotary.gradle.plugin.platformSourcesRootPath
import com.ing.zknotary.gradle.plugin.zkNotaryExtension
import com.ing.zknotary.gradle.zinc.template.TemplateRenderer
import com.ing.zknotary.gradle.zinc.template.parameters.SerializedStateTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.SignersTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.StateGroupTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.TxStateTemplateParameters
import com.ing.zknotary.gradle.zinc.util.CircuitConfigurator
import com.ing.zknotary.gradle.zinc.util.CodeGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @Suppress("NestedBlockDepth")
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
                    configurator.circuitConfiguration.circuit.states
                        .forEach { addConfigurations(TxStateTemplateParameters(it)) }

                    addConfigurations(SignersTemplateParameters(configurator.circuitConfiguration.groups.signerGroup))

                    configurator.circuitConfiguration.groups.inputGroup.forEach { stateGroup ->
                        if (stateGroup.stateGroupSize > 0)
                            addConfigurations(SerializedStateTemplateParameters("input", stateGroup))
                    }
                    addConfigurations(StateGroupTemplateParameters("input", configurator.circuitConfiguration.groups.inputGroup))

                    configurator.circuitConfiguration.groups.outputGroup.forEach { stateGroup ->
                        if (stateGroup.stateGroupSize > 0)
                            addConfigurations(SerializedStateTemplateParameters("output", stateGroup))
                    }
                    addConfigurations(StateGroupTemplateParameters("output", configurator.circuitConfiguration.groups.outputGroup))

                    configurator.circuitConfiguration.groups.referenceGroup.forEach { stateGroup ->
                        if (stateGroup.stateGroupSize > 0)
                            addConfigurations(SerializedStateTemplateParameters("reference", stateGroup))
                    }
                    addConfigurations(StateGroupTemplateParameters("reference", configurator.circuitConfiguration.groups.referenceGroup))
                }.resolveAllTemplateParameters()
                .forEach(templateRenderer::renderTemplate)
        }
    }
}
