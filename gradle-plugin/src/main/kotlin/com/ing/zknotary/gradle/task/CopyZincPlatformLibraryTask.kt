package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.ZincSourcesCopier
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.platformLibraries
import com.ing.zknotary.gradle.util.zkNotaryExtension
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
