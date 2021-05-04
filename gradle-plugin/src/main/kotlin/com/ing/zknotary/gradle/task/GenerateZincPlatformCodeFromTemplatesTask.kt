package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.plugin.circuitNames
import com.ing.zknotary.gradle.plugin.getTemplateContents
import com.ing.zknotary.gradle.plugin.zkNotaryExtension
import com.ing.zknotary.gradle.zinc.template.TemplateRenderer
import com.ing.zknotary.gradle.zinc.util.CodeGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        val extension = project.zkNotaryExtension

        project.circuitNames?.forEach { circuitName ->
            val circuitSourceOutputPath = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src")
            val codeGenerator = CodeGenerator(circuitSourceOutputPath)
            val consts = extension.circuitSourcesBasePath.resolve(circuitName).resolve("consts.zn").readText()

            codeGenerator.generateMerkleUtilsCode(project.getTemplateContents(extension.merkleTemplate), consts)
            codeGenerator.generateMainCode(project.getTemplateContents(extension.mainTemplate), consts)

            val templateRenderer = TemplateRenderer(circuitSourceOutputPath.toPath()) { params ->
                project.getTemplateContents(params.templateFile)
            }
            extension.resolveAllTemplateParameters().forEach {
                templateRenderer.renderTemplate(it)
            }
        }
    }
}
