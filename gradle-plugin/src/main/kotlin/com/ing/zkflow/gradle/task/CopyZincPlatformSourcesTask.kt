package com.ing.zkflow.gradle.task

import com.ing.zkflow.compilation.zinc.util.ZincSourcesCopier
import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.platformSources
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CopyZincPlatformSourcesTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkFlowExtension

        project.circuitNames?.forEach { circuitName ->
            val copier = ZincSourcesCopier(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            copier.copyZincPlatformSources(project.platformSources)
        }
    }
}
