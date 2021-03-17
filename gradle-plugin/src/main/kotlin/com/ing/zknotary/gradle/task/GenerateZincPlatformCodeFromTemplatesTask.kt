package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.Templates
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
            val template = Templates(circuitName, extension.mergedCircuitOutputPath, extension.circuitSourcesBasePath)

            template.templateContents = project.getTemplateContents("floating_point.zn").readText()
            template.generateFloatingPointsCode(extension.bigDecimalSizes)

            template.templateContents = project.getTemplateContents("merkle_template.zn").readText()
            template.generateMerkleUtilsCode()

            template.templateContents = project.getTemplateContents("main_template.zn").readText()
            template.generateMainCode()
        }
    }
}
