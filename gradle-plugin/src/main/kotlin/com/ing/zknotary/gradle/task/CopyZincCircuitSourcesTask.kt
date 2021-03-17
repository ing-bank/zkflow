package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.Copy
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CopyZincCircuitSourcesTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkNotaryExtension

        project.circuitNames?.forEach { circuitName ->
            val copy = Copy(circuitName, extension.mergedCircuitOutputPath, extension.circuitSourcesBasePath)
            copy.createCopyZincCircuitSources(project.version.toString())
        }
    }
}
