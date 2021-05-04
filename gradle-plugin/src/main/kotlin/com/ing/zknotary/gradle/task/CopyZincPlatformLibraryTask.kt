package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.plugin.circuitNames
import com.ing.zknotary.gradle.plugin.platformLibraries
import com.ing.zknotary.gradle.plugin.zkNotaryExtension
import com.ing.zknotary.gradle.zinc.util.ZincSourcesCopier
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
