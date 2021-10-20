package com.ing.zkflow.gradle.task

import com.ing.zkflow.compilation.joinConstFiles
import com.ing.zkflow.compilation.zinc.template.TemplateRenderer
import com.ing.zkflow.compilation.zinc.template.parameters.SerializedStateTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.SignersTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.StateGroupTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.TxStateTemplateParameters
import com.ing.zkflow.compilation.zinc.util.CircuitConfigurator
import com.ing.zkflow.compilation.zinc.util.CodeGenerator
import com.ing.zkflow.gradle.extension.ZKFlowExtension
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
            val circuitSources = extension.circuitSourcesBasePath.resolve(circuitName)
            val configurator = CircuitConfigurator.fromSources(circuitSources, ZKFlowExtension.CONFIG_CIRCUIT_FILE)

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

                    configurator.circuitConfiguration.groups.inputGroup.filter { it.stateGroupSize > 0 }.forEach { stateGroup ->
                        addConfigurations(SerializedStateTemplateParameters("input", stateGroup))
                    }
                    addConfigurations(StateGroupTemplateParameters("input", configurator.circuitConfiguration.groups.inputGroup))

                    configurator.circuitConfiguration.groups.outputGroup.filter { it.stateGroupSize > 0 }.forEach { stateGroup ->
                        addConfigurations(SerializedStateTemplateParameters("output", stateGroup))
                    }
                    addConfigurations(StateGroupTemplateParameters("output", configurator.circuitConfiguration.groups.outputGroup))

                    configurator.circuitConfiguration.groups.referenceGroup.filter { it.stateGroupSize > 0 }.forEach { stateGroup ->
                        addConfigurations(SerializedStateTemplateParameters("reference", stateGroup))
                    }
                    addConfigurations(StateGroupTemplateParameters("reference", configurator.circuitConfiguration.groups.referenceGroup))
                }.resolveAllTemplateParameters()
                .forEach(templateRenderer::renderTemplate)
        }
    }
}
