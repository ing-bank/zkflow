package com.ing.zkflow.gradle.task

import com.ing.zkflow.compilation.zinc.util.ZincSourcesCopier
import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.zincCommonFolderName
import com.ing.zkflow.gradle.plugin.zincStatesFolderName
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

open class CopyZincCircuitSourcesTask : DefaultTask() {
    private val extension = project.zkFlowExtension

    @InputDirectory
    val zincSources = extension.circuitSourcesBasePath

    @OutputDirectories
    val zincOutputs = project.circuitNames?.map { extension.mergedCircuitOutputPath.resolve(it).resolve("src") }

    @TaskAction
    fun createCopyZincSources() {
        project.circuitNames?.forEach { circuitName ->
            val circuitSourceOutputPath = extension
                .mergedCircuitOutputPath
                .resolve(circuitName)
                .resolve("src")
            val copier = ZincSourcesCopier(circuitSourceOutputPath)

            listOfNotNull(
                circuitName,
                project.zincCommonFolderName,
                project.zincStatesFolderName,
            ).forEach {
                copier.copyZincCircuitSources(
                    extension.circuitSourcesBasePath.resolve(it),
                    it,
                    project.version.toString()
                )
            }
        }
    }
}
