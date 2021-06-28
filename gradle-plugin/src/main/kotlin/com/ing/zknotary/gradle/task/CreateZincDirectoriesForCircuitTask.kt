package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.plugin.platformSkeletonCircuit
import com.ing.zknotary.gradle.plugin.platformSkeletonState
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

        // skeleton-states -> statesSourcesPath
        project.copy { copy ->
            copy.from(project.platformSkeletonState).eachFile {
                println(it.sourcePath)
                @Suppress("SpreadOperator")
                it.relativePath = RelativePath(true, *it.relativePath.segments.drop(2).toTypedArray())
            }
            copy.into(extension.circuitSourcesBasePath.resolve(extension.statesSourcesPath))
            copy.includeEmptyDirs = false
        }

        // skeleton-circuit -> circuitName
        project.copy { copy ->
            copy.from(project.platformSkeletonCircuit).eachFile {
                @Suppress("SpreadOperator")
                it.relativePath = RelativePath(true, *it.relativePath.segments.drop(2).toTypedArray())
            }
            copy.into(extension.circuitSourcesBasePath.resolve(circuitName))
            copy.includeEmptyDirs = false
        }
    }
}
