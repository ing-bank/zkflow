package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.task.CopyZincCircuitSourcesTask
import com.ing.zknotary.gradle.task.CopyZincPlatformSourcesTask
import com.ing.zknotary.gradle.task.CreateZincDirectoriesForInputCommandTask
import com.ing.zknotary.gradle.task.GenerateZincPlatformCodeFromTemplatesTask
import com.ing.zknotary.gradle.task.PrepareCircuitForCompilationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

@Suppress("unused")
class ZKNotaryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(ZKNotaryExtension.NAME, ZKNotaryExtension::class.java, project)

        project.plugins.withType(JavaPlugin::class.java) {
            // Add the required dependencies to consumer projects
            project.configurations.create("zinc")
            project.dependencies.add(
                "zinc",
                "com.ing.zknotary:zinc-platform-sources:${extension.zincPlatformSourcesVersion}"
            )
            project.dependencies.add("implementation", "com.ing.zknotary:notary:${extension.notaryVersion}")
        }

        project.tasks.create("copyZincCircuitSources", CopyZincCircuitSourcesTask::class.java)
        project.tasks.create("createZincDirectoriesForInputCommand", CreateZincDirectoriesForInputCommandTask::class.java)
        project.tasks.create("copyZincPlatformSources", CopyZincPlatformSourcesTask::class.java)
        project.tasks.create("generateZincPlatformCodeFromTemplates", GenerateZincPlatformCodeFromTemplatesTask::class.java)
        project.tasks.create("prepareCircuitForCompilation", PrepareCircuitForCompilationTask::class.java)
    }
}
