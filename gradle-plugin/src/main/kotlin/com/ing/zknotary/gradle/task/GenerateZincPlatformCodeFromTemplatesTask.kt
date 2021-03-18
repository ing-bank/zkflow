package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.Renderer
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.platformTemplates
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        val extension = project.zkNotaryExtension

        project.circuitNames?.forEach { circuitName ->
            val renderer = Renderer(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            val consts = extension.circuitSourcesBasePath.resolve(circuitName).resolve("consts.zn").readText()

            renderer.operateTemplateRenderer(project.platformTemplates, consts, extension.bigDecimalSizes)
        }
    }
}
