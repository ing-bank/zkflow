package com.ing.zkflow.gradle.task

import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import com.ing.zkflow.zinc.poet.generate.CIRCUIT
import com.ing.zkflow.zinc.poet.generate.SRC_DIR
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

open class ZincPoetTask : DefaultTask() {
    private val extension = project.zkFlowExtension

    @InputDirectory
    val zincSources = extension.circuitSourcesBasePath

    @OutputDirectories
    val zincOutputs = project.circuitNames?.map { extension.mergedCircuitOutputPath.resolve(it).resolve("src") }

    private val circuitNames = project.circuitNames ?: emptyList()

    @TaskAction
    @Suppress("SpreadOperator")
    fun generateZincCircuits() {
        project.javaexec {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            val main = javaPlugin.sourceSets.findByName("main") ?: error("Can't find main sourceSet")

            it.main = "com.ing.zkflow.zinc.poet.generate.ZincPoetKt"
            val generatedKspResources = project.files(project.buildDir.resolve("generated/ksp/main/resources"))
            it.classpath = main.runtimeClasspath.plus(generatedKspResources)
            it.args(
                "$SRC_DIR=$zincSources",
                *circuitNames.map { "$CIRCUIT=$it" }.toTypedArray()
            )
        }
    }
}
