package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.platformSources
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

abstract class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        generateFloatingPointCode()
    }

    fun generateFloatingPointCode(){
        val templateContents = project.getTemplateContents("floating_point.zn")

        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            extension.bigDecimalSizes.forEach {
                val floatingPointContent = templateContents.replace("\${INTEGER_SIZE_PLACEHOLDER}", it.first.toString())
                    .replace("\${FRACTION_SIZE_PLACEHOLDER}", it.second.toString())
                val sizeSuffix = "${it.first}_${it.second}"
                val targetFile = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src/floating_point_$sizeSuffix.zn")
                targetFile.delete()
                targetFile.createNewFile()
                targetFile.writeBytes(floatingPointContent.toByteArray())
            }
        }
    }

    fun generateMerkleUtilsCode(){

    }




    private fun Project.getTemplateContents(templateFileName: String): String {
        return project.platformSources.matching {
            it.include("zinc-platform-templates/$templateFileName")
        }.singleFile.readText()
    }
}
