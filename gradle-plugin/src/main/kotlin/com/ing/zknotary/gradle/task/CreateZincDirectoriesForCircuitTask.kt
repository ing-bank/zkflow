package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.plugin.platformSamples
import com.ing.zknotary.gradle.plugin.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
This task receives the input circuitName as a command line argument and creates a zinc directory under src/main with the
provided circuit name. It also copies sample code for consts, contract rules, and contract state that should be manually
implemented on zkdapp side.
 **/
open class CreateZincDirectoriesForCircuitTask : DefaultTask() {
    private var circuitName = "circuit"

    @Option(option = "circuit", description = "Create folders for a circuit.")
    fun setCircuitName(circuitName: String) {
        this.circuitName = circuitName
    }

    @Input
    fun getCircuitName(): String = circuitName

    @TaskAction
    fun createZincDirectoriesForCircuit() {
        val extension = project.zkNotaryExtension

        project.copy { copy ->
            copy.from(project.platformSamples).eachFile {
                @Suppress("SpreadOperator")
                it.relativePath = RelativePath(true, *it.relativePath.segments.drop(1).toTypedArray())
            }
            copy.into(extension.circuitSourcesBasePath)
            copy.includeEmptyDirs = false
        }
    }
}
