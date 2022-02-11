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
import java.io.File

open class GenerateZincCircuitsTask : DefaultTask() {
    private val extension = project.zkFlowExtension

    @InputDirectory
    val zincSources = extension.circuitSourcesBasePath

    @InputDirectory
    val generatedKspResources: File = project.buildDir.resolve("generated/ksp/main/resources")

    @OutputDirectories
    val zincOutputs = project.circuitNames?.map { extension.mergedCircuitOutputPath.resolve(it).resolve("src") }

    private val circuitNames = project.circuitNames ?: emptyList()

    @TaskAction
    @Suppress("SpreadOperator")
    fun generateZincCircuits() {
        project.javaexec {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            val main = javaPlugin.sourceSets.findByName("main") ?: error("Can't find main sourceSet")

            it.main = "com.ing.zkflow.zinc.poet.generate.GenerateZincCircuitsKt"
            // We need to add 'build/generated/ksp/src/main/resources' to the main sourceSet, because otherwise
            // the generated META-INF/services file is not picked up by the `zincPoet` task.
            // It would be nicest if KSP already did this, however it doesn't.
            it.classpath = main.runtimeClasspath + project.files(generatedKspResources)
            it.args(
                "$SRC_DIR=$zincSources",
                *circuitNames.map { "$CIRCUIT=$it" }.toTypedArray()
            )
        }
    }
}
