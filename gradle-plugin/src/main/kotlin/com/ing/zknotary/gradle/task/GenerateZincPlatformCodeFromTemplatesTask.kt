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
            val renderer = TemplateRenderer(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))

            renderer.generateFloatingPointsCode(
                project.getTemplateContents("floating_point.zn").readText(),
                extension.bigDecimalSizes
            )

            val consts = extension.circuitSourcesBasePath.resolve(circuitName).resolve("consts.zn").readText()

            renderer.generateMerkleUtilsCode(
                project.getTemplateContents("merkle_template.zn").readText(),
                consts
            )

            renderer.generateMainCode(
                project.getTemplateContents("main_template.zn").readText(),
                consts
            )
        }
    }
}
