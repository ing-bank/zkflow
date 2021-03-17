package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.Copy
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.platformSources
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class CopyZincPlatformSourcesTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkNotaryExtension
        val platformSources = project.platformSources.matching { it.include("zinc-platform-sources/**/*.zn") }.toList().toTypedArray()

        project.circuitNames?.forEach { circuitName ->
            val copy = Copy(circuitName, extension.mergedCircuitOutputPath, extension.circuitSourcesBasePath)
            copy.createCopyZincPlatformSources(platformSources)
        }
    }
}
