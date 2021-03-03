package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.platformSources
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class CopyZincCircuitSamplesTask : DefaultTask() {
    private var circuit = "move" // TODO remove this

    @Option(option = "command", description = "Set the command type of the transaction.")
    fun setCommand(command: String) {
        circuit = command
    }

    @Input
    fun getCommand(): String {
        return circuit
    }

    @TaskAction
    fun copyZincCircuitSamples() {

        val extension = project.zkNotaryExtension
        val samples = project.platformSources.matching { it.include("zinc-platform-samples/**/*.zn") }

        project.copy { copy ->
            copy.into(extension.circuitSourcesBasePath.resolve(circuit))
            copy.from(samples).eachFile {
                @Suppress("SpreadOperator")
                it.relativePath = RelativePath(true, *it.relativePath.segments.drop(1).toTypedArray())
            }
            copy.includeEmptyDirs = false
        }
    }
}
