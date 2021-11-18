package com.ing.zkflow.gradle.plugin

import com.ing.zkflow.gradle.extension.ZKFlowExtension
import com.ing.zkflow.gradle.task.CopyZincCircuitSourcesForTestsTask
import com.ing.zkflow.gradle.task.CopyZincCircuitSourcesTask
import com.ing.zkflow.gradle.task.CopyZincPlatformSourcesAndLibraryTask
import com.ing.zkflow.gradle.task.CreateZincDirectoriesForCircuitTask
import com.ing.zkflow.gradle.task.GenerateZincPlatformCodeFromTemplatesTask
import com.ing.zkflow.gradle.task.PrepareCircuitForCompilationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import java.io.File
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
            project.dependencies.add("implementation", "com.ing.zkflow:compilation:${extension.notaryVersion}")
            project.dependencies.add("implementation", "com.ing.zkflow:annotations:${extension.notaryVersion}")

            project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            // KSP.
            project.pluginManager.apply("com.google.devtools.ksp")
            project.dependencies.add("ksp", "com.ing.zkflow:compiler-plugin-ksp:${extension.notaryVersion}")

            // Arrow.
            val log = File("/tmp/apply-plugin.log")
            log.appendText("Hello\n")

            project
                .tasks
                .filterIsInstance<KotlinCompile<*>>()
                .forEach { task ->
                    log.appendText("${task.name}\n")
                    log.appendText("Before: \n\t ${task.kotlinOptions.freeCompilerArgs.joinToString(separator = "\n\t") { it }}\n")

                    // TODO this must be resolved differently, only works because `zkdapp-tester` includes the parent build.
                    val libPath = File(System.getProperty("user.dir"))
                        .resolve("..")
                        .resolve("compiler-plugin-arrow/build/libs/compiler-plugin-arrow-${extension.notaryVersion}.jar")

                    task.kotlinOptions.freeCompilerArgs += "-Xplugin=$libPath"

                    log.appendText("After: \n\t ${task.kotlinOptions.freeCompilerArgs.joinToString(separator = "\n\t") { it }}\n")
                }

            project.dependencies.add("implementation", "com.ing.zkflow:serialization-candidate:${extension.notaryVersion}")
        }

        val createZincDirsForCircuitTask = project.tasks.create(
            "createZincDirectoriesForCircuit",
            CreateZincDirectoriesForCircuitTask::class.java
        )

        val copyCircuitTask = project.tasks.create("copyZincCircuitSources", CopyZincCircuitSourcesTask::class.java)
        val copyPlatformTask = project.tasks.create("copyZincPlatformSourcesAndLibs", CopyZincPlatformSourcesAndLibraryTask::class.java)

        val generateFromTemplatesTask = project.tasks.create(
            "generateZincPlatformCodeFromTemplates",
            GenerateZincPlatformCodeFromTemplatesTask::class.java
        )

        val prepareForCompilationTask = project.tasks.create("prepareCircuitForCompilation", PrepareCircuitForCompilationTask::class.java)
        val copyZincCircuitSourcesForTestsTask =
            project.tasks.create("copyZincCircuitSourcesForTests", CopyZincCircuitSourcesForTestsTask::class.java)

        // If a new circuit is scaffolded, the processing tasks should run after it
        copyCircuitTask.mustRunAfter(createZincDirsForCircuitTask)
        copyPlatformTask.mustRunAfter(createZincDirsForCircuitTask)
        generateFromTemplatesTask.mustRunAfter(createZincDirsForCircuitTask)
        generateFromTemplatesTask
            .dependsOn("compileKotlin") // So the command metadata can be found
            .mustRunAfter("compileKotlin")
        prepareForCompilationTask.mustRunAfter(createZincDirsForCircuitTask)

        prepareForCompilationTask.dependsOn(copyCircuitTask, copyPlatformTask, generateFromTemplatesTask)
        prepareForCompilationTask.mustRunAfter(copyCircuitTask, copyPlatformTask, generateFromTemplatesTask)

        project.tasks.create("processZincSources") {
            it.dependsOn(copyPlatformTask)
            it.dependsOn(copyCircuitTask)
            it.dependsOn(generateFromTemplatesTask)
            it.dependsOn(prepareForCompilationTask)
        }

        project.tasks.getByPath("assemble").dependsOn("processZincSources")

        copyZincCircuitSourcesForTestsTask.dependsOn("assemble", "processTestResources")
        copyZincCircuitSourcesForTestsTask.mustRunAfter("assemble", "processTestResources")

        project.afterEvaluate {
            it.tasks.getByPath("test")
                .dependsOn("copyZincCircuitSourcesForTests")
                .mustRunAfter("copyZincCircuitSourcesForTests")
        }
    }
}
