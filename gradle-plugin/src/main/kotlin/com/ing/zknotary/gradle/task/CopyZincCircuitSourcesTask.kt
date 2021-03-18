package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.CopyRenderer
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CopyZincCircuitSourcesTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkNotaryExtension

        project.circuitNames?.forEach { circuitName ->
            val copy = CopyRenderer(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            copy.createCopyZincCircuitSources(extension.circuitSourcesBasePath.resolve(circuitName), circuitName, project.version.toString())
        }
    }
}
