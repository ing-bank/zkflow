package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.TemplateRenderer
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.getTemplateContents
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        val extension = project.zkNotaryExtension

        project.circuitNames?.forEach { circuitName ->
            val renderer = TemplateRenderer(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src")) {
                project.getTemplateContents(it)
            }

            val consts = extension.circuitSourcesBasePath.resolve(circuitName).resolve("consts.zn").readText()

            renderer.generateMerkleUtilsCode(project.getTemplateContents(extension.merkleTemplate), consts)
            renderer.generateMainCode(project.getTemplateContents(extension.mainTemplate), consts)

            extension.resolveAllTemplateParameters().forEach {
                renderer.generateTemplate(project.getTemplateContents(it.templateFile), it)
            }
        }
    }
}
