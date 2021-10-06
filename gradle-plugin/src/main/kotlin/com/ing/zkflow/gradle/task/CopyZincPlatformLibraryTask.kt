package com.ing.zkflow.gradle.task

import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.platformLibraries
import com.ing.zkflow.gradle.plugin.zkNotaryExtension
import com.ing.zkflow.gradle.zinc.util.ZincSourcesCopier
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CopyZincPlatformLibraryTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkNotaryExtension

        project.circuitNames?.forEach { circuitName ->
            val copier = ZincSourcesCopier(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            copier.copyZincPlatformSources(project.platformLibraries)
        }
    }
}
