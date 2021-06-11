package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.plugin.circuitNames
import com.ing.zknotary.gradle.plugin.zincCommonFolderName
import com.ing.zknotary.gradle.plugin.zkNotaryExtension
import com.ing.zknotary.gradle.zinc.util.ZincSourcesCopier
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CopyZincCircuitSourcesTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkNotaryExtension
        val commonFolderName = project.zincCommonFolderName

        project.circuitNames?.filterNot { it.contains(extension.statesSourcesPath) }?.forEach { circuitName ->
            val circuitSourceOutputPath = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src")
            val copier = ZincSourcesCopier(circuitSourceOutputPath)

            listOfNotNull(
                circuitName,
                commonFolderName,
            ).forEach {
                copier.copyZincCircuitSources(extension.circuitSourcesBasePath.resolve(it), it, project.version.toString())
            }
        }
    }
}
