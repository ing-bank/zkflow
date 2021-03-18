package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.CopyRenderer
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.platformSources
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CopyZincPlatformSourcesTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkNotaryExtension

        project.circuitNames?.forEach { circuitName ->
            val copy = CopyRenderer(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            copy.createCopyZincPlatformSources(project.platformSources)
        }
    }
}
