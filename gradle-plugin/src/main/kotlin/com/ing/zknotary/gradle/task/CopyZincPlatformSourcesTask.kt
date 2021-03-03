package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.platformSources
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.TaskAction

abstract class CopyZincPlatformSourcesTask : DefaultTask() {

    @TaskAction
    fun createCopyZincCircuitSources() {
        val extension = project.zkNotaryExtension
        val sources = project.platformSources.matching { it.include("zinc-platform-sources/**/*.zn") }
        project.circuitNames?.forEach { circuitName ->
            project.copy { copy ->
                copy.into(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
                copy.from(sources).eachFile {
                    @Suppress("SpreadOperator")
                    it.relativePath = RelativePath(true, *it.relativePath.segments.drop(1).toTypedArray())
                }
                copy.includeEmptyDirs = false
            }
        }
    }
}
