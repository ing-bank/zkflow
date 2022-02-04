package com.ing.zkflow.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

open class ZincPoetTask : DefaultTask() {
    // private val extension = project.zkFlowExtension

    @InputDirectory
    internal var inputs = "src/main/kotlin"

    @OutputDirectory
    internal var outputs = "build/zinc"

    @TaskAction
    fun generateZincCircuits() {
        project.javaexec {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            val main = javaPlugin.sourceSets.findByName("main") ?: error("Can't find main sourceSet")

            it.main = "com.ing.zkflow.zinc.poet.generate.ZincPoetKt"
            val generatedKspResources = project.files(project.buildDir.resolve("generated/ksp/main/resources"))
            it.classpath = main.runtimeClasspath.plus(generatedKspResources)
            it.args()
        }
    }
}
