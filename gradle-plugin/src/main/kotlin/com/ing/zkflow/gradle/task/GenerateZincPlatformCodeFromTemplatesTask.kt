package com.ing.zkflow.gradle.task

import com.ing.zkflow.gradle.plugin.platformSourcesRootPath
import com.ing.zkflow.gradle.plugin.platformTemplatesRootPath
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

open class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {
    private val extension = project.zkFlowExtension

    init {
        // used to have the actual zinc build dirs as output dirs, but that would make this task always out of date.
        // We really only care about outdated inputs.
        this.outputs.upToDateWhen { false }
    }

    @InputFiles
    val platformSource: Configuration = project.configurations.getByName("zinc")

    @InputDirectory
    val zincSources = extension.circuitSourcesBasePath

    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        project.javaexec {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            val main = javaPlugin.sourceSets.findByName("main") ?: error("Can't find main sourceset")

            it.main = "com.ing.zkflow.compilation.GenerateZincPlatformCodeFromTemplatesJavaExecTaskKt"
            it.classpath = main.runtimeClasspath
            it.args(
                project.platformSourcesRootPath.absolutePath,
                project.platformTemplatesRootPath.absolutePath,
                project.projectDir.resolve(extension.circuitSourcesBasePath).absolutePath,
                project.buildDir.resolve(extension.mergedCircuitOutputPath).absolutePath,
                extension.zkFlowTemplateConfigurationClassName
            )
        }
    }
}
