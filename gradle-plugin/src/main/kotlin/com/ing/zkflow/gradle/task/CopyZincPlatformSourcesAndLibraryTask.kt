package com.ing.zkflow.gradle.task

import com.ing.zkflow.compilation.zinc.util.ZincSourcesCopier
import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.platformLibraries
import com.ing.zkflow.gradle.plugin.platformSources
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

open class CopyZincPlatformSourcesAndLibraryTask : DefaultTask() {
    private val extension = project.zkFlowExtension

    @InputFiles
    val platformSource: Configuration = project.configurations.getByName("zinc")

    @OutputDirectories
    val zincOutputs = project.circuitNames?.map { extension.mergedCircuitOutputPath.resolve(it).resolve("src") }

    @TaskAction
    fun createCopyZincSources() {
        project.circuitNames?.forEach { circuitName ->
            val copier = ZincSourcesCopier(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            copier.copyZincPlatformSources(project.platformSources)
            copier.copyZincPlatformSources(project.platformLibraries)
        }
    }
}
