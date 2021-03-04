package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BuildZincCircuitTask : DefaultTask() {

    @TaskAction
    fun compileZinc() {
        removeDebugCode()
    }

    private fun removeDebugCode() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            File("${extension.mergedCircuitOutputPath.resolve(circuitName)}/").walk().forEach { file ->
                if (file.name.contains(".zn")) {
                    val lines = file.readLines()
                    file.delete()
                    file.createNewFile()

                    lines.filter { s: String -> !s.contains("dbg!") }.map {
                        file.appendText("$it\n")
                    }
                }
            }
        }
    }
}
