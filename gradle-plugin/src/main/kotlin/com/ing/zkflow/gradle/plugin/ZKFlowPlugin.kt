package com.ing.zkflow.gradle.plugin

import com.ing.zkflow.gradle.extension.ZKFlowExtension
import com.ing.zkflow.gradle.task.CopyZincCircuitSourcesForTestsTask
import com.ing.zkflow.gradle.task.CopyZincCircuitSourcesTask
import com.ing.zkflow.gradle.task.CopyZincPlatformLibraryTask
import com.ing.zkflow.gradle.task.CopyZincPlatformSourcesTask
import com.ing.zkflow.gradle.task.CreateZincDirectoriesForCircuitTask
import com.ing.zkflow.gradle.task.GenerateZincPlatformCodeFromTemplatesTask
import com.ing.zkflow.gradle.task.PrepareCircuitForCompilationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import java.net.URI

@Suppress("unused")
class ZKFlowPlugin : Plugin<Project> {
    @Suppress("LongMethod")
    override fun apply(project: Project) {
        val extension = project.extensions.create(ZKFlowExtension.NAME, ZKFlowExtension::class.java, project)

        project.repositories.apply {
            // For kotlinx.serialization plugin
            maven { it.url = URI.create("https://plugins.gradle.org/m2/") }

            // For BFL
            maven {
                it.name = "BinaryFixedLengthSerializationRepo"
                it.url = URI.create("https://maven.pkg.github.com/ingzkp/kotlinx-serialization-bfl")
                it.credentials { credentials ->
                    credentials.username = System.getenv("GITHUB_USERNAME")
                    credentials.password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
        project.plugins.withType(JavaPlugin::class.java) {
            // Add the required dependencies to consumer projects
            project.configurations.create("zinc")
            project.dependencies.add(
                "zinc",
                "com.ing.zkflow:zinc-platform-sources:${extension.zincPlatformSourcesVersion}"
            )
            project.dependencies.add("implementation", "com.ing.zkflow:protocol:${extension.notaryVersion}")

            project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            project.pluginManager.apply("com.google.devtools.ksp")
            project.dependencies.add("ksp", "com.ing.zkflow:zktransaction-compiler-plugin:${extension.notaryVersion}")
        }

        val createZincDirsForCircuitTask = project.tasks.create(
            "createZincDirectoriesForCircuit",
            CreateZincDirectoriesForCircuitTask::class.java
        )

        val copyCircuitTask = project.tasks.create("copyZincCircuitSources", CopyZincCircuitSourcesTask::class.java)
        val copyPlatformTask = project.tasks.create("copyZincPlatformSources", CopyZincPlatformSourcesTask::class.java)
        val copyPlatformLibsTask =
            project.tasks.create("copyZincPlatformLibraries", CopyZincPlatformLibraryTask::class.java)
        val generateFromTemplatesTask = project.tasks.create(
            "generateZincPlatformCodeFromTemplates",
            GenerateZincPlatformCodeFromTemplatesTask::class.java
        )
        val prepareForCompilationTask =
            project.tasks.create("prepareCircuitForCompilation", PrepareCircuitForCompilationTask::class.java)
        val copyZincCircuitSourcesForTestsTask =
            project.tasks.create("copyZincCircuitSourcesForTests", CopyZincCircuitSourcesForTestsTask::class.java)

        // If a new circuit is scaffolded, the processing tasks should run after it
        copyCircuitTask.mustRunAfter(createZincDirsForCircuitTask)
        copyPlatformTask.mustRunAfter(createZincDirsForCircuitTask)
        copyPlatformLibsTask.mustRunAfter(createZincDirsForCircuitTask)
        generateFromTemplatesTask.mustRunAfter(createZincDirsForCircuitTask)
        prepareForCompilationTask.mustRunAfter(createZincDirsForCircuitTask)

        prepareForCompilationTask.dependsOn(copyCircuitTask, copyPlatformTask, generateFromTemplatesTask)
        prepareForCompilationTask.mustRunAfter(copyCircuitTask, copyPlatformTask, generateFromTemplatesTask)

        project.tasks.create("processZincSources") {
            it.dependsOn("copyZincPlatformLibraries")
            it.dependsOn("copyZincPlatformSources")
            it.dependsOn("generateZincPlatformCodeFromTemplates")
            it.dependsOn("prepareCircuitForCompilation")
            it.dependsOn("copyZincCircuitSources")
        }

        project.tasks.getByPath("assemble").dependsOn("processZincSources")

        copyZincCircuitSourcesForTestsTask.dependsOn("assemble", "processTestResources")
        copyZincCircuitSourcesForTestsTask.mustRunAfter("assemble", "processTestResources")

        project.afterEvaluate {
            it.tasks.getByPath("test").dependsOn("copyZincCircuitSourcesForTests").mustRunAfter("copyZincCircuitSourcesForTests")
        }
    }
}
