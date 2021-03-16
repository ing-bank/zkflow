package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.generateFloatingPointsCode
import com.ing.zknotary.gradle.util.generateMainCode
import com.ing.zknotary.gradle.util.generateMerkleUtilsCode
import com.ing.zknotary.gradle.util.platformSources
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        val extension = project.zkNotaryExtension
        val floatingPointTemplateContents = project.getTemplateContents("floating_point.zn")
        val merkleTemplateContents = project.getTemplateContents("merkle_template.zn")
        val mainTemplateContents = project.getTemplateContents("main_template.zn")

        project.circuitNames?.forEach { circuitName ->
            val targetFilePath = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src").absolutePath
            val constsContent = File("${extension.circuitSourcesBasePath.resolve(circuitName)}/consts.zn").readText()

            generateFloatingPointsCode(targetFilePath, floatingPointTemplateContents, extension.bigDecimalSizes)
            generateMerkleUtilsCode(targetFilePath, merkleTemplateContents, constsContent)
            generateMainCode(targetFilePath, mainTemplateContents, constsContent, circuitName)
        }
    }

    private fun Project.getTemplateContents(templateFileName: String): String {
        return project.platformSources.matching {
            it.include("zinc-platform-templates/$templateFileName")
        }.singleFile.readText()
    }
}
