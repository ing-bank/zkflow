package com.ing.zkflow.gradle.task

import com.ing.zkflow.compilation.zinc.util.ZincSourcesCopier
import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.platformLibraries
import com.ing.zkflow.gradle.plugin.platformSources
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

open class CopyZincPlatformSourcesAndLibraryTask : DefaultTask() {
    private val extension = project.zkFlowExtension

    init {
        // used to have the actual zinc build dirs as output dirs, but that would make this task always out of date.
        // We really only care about outdated inputs.
        this.outputs.upToDateWhen { true }
    }

    @InputFiles
    val platformSource: Configuration = project.configurations.getByName("zinc")

    @TaskAction
    fun createCopyZincSources() {
        project.circuitNames?.forEach { circuitName ->
            val copier = ZincSourcesCopier(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            copier.copyZincPlatformSources(project.platformSources)
            copier.copyZincPlatformSources(project.platformLibraries)
        }
    }
}
