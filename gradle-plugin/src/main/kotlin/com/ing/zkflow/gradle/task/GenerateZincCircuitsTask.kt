package com.ing.zkflow.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction
import java.io.File

open class GenerateZincCircuitsTask : DefaultTask() {
    private val generatedKspResources: File = project.buildDir.resolve("generated/ksp/main/resources")

    @TaskAction
    @Suppress("SpreadOperator")
    fun generateZincCircuits() {
        project.javaexec {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            val main = javaPlugin.sourceSets.findByName("main") ?: error("Can't find main sourceSet")

            it.main = "com.ing.zkflow.zinc.poet.generate.GenerateZincCircuitsKt"
            // We must add 'build/generated/ksp/src/main/resources' to the main sourceSet if it exists, because
            // otherwise the generated META-INF/services file is not picked up by the `generateZincCircuits` task.
            // It would be the nicest if KSP already did this, however it doesn't.
            it.classpath = main.runtimeClasspath + project.files(generatedKspResources).filter(File::exists)

            println("--> Pay attention!!! We're gonna generate some zinc now!")
        }
    }
}
