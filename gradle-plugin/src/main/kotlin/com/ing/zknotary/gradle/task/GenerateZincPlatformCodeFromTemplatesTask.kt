package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.Templates
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.getPlatformTemplatesPath
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        val extension = project.zkNotaryExtension
        val platformTemplatesPath = File(project.getPlatformTemplatesPath())

        project.circuitNames?.forEach { circuitName ->
            val template = Templates(circuitName, extension.mergedCircuitOutputPath, extension.circuitSourcesBasePath, platformTemplatesPath)

            template.generateFloatingPointsCode(extension.bigDecimalSizes)
            template.generateMerkleUtilsCode()
            template.generateMainCode()
        }
    }
}
