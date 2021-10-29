package com.ing.zkflow.gradle.task

import com.ing.zkflow.gradle.plugin.platformSourcesRootPath
import com.ing.zkflow.gradle.plugin.platformTemplatesRootPath
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction

open class GenerateZincPlatformCodeFromTemplatesTask : DefaultTask() {
    @TaskAction
    fun generateZincPlatformCodeFromTemplates() {
        project.javaexec {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            val main = javaPlugin.sourceSets.findByName("main") ?: error("Can't find main sourceset")
            val extension = project.zkFlowExtension

            it.main = "com.ing.zkflow.compilation.GenerateZincPlatformCodeFromTemplatesJavaExecTaskKt"
            it.classpath = main.runtimeClasspath
            it.args(
                project.platformSourcesRootPath.absolutePath,
                project.platformTemplatesRootPath.absolutePath,
                project.projectDir.resolve(extension.circuitSourcesBasePath).absolutePath,
                project.buildDir.resolve(extension.mergedCircuitOutputPath).absolutePath,
                extension.zkFlowTemplateConfigurationClass
            )
        }
    }
}
