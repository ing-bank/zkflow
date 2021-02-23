package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.ide.idea.IdeaPlugin

@Suppress("unused")
class ZKNotaryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("zkp", ZKNotaryExtension::class.java)

        // For each consumer project, mark the KSP generated code as source
        // Load the KSP plugin for consumers
        project.plugins.apply("symbol-processing")

        val generatedSourcePath = "${project.buildDir.name}/generated/ksp/main/kotlin"
        val generatedTestSourcePath = "${project.buildDir.name}/generated/ksp/test/kotlin"

        project.plugins.withType(JavaPlugin::class.java) {
            val sourceSets = project.properties["sourceSets"] as SourceSetContainer?
            // Only add to the sourcesets if the Java plugin is loaded for the project
            sourceSets!!.getByName("main").java.srcDir(generatedSourcePath)
            sourceSets.getByName("test").java.srcDir(generatedTestSourcePath)

            // Add the required dependencies to consumer projects
            project.configurations.create("zinc")
            project.dependencies.add(
                "zinc",
                "com.ing.zknotary:zinc-platform-sources:${extension.zincPlatformSourcesVersion}"
            )
            project.dependencies.add("implementation", "com.ing.zknotary:notary:${extension.notaryVersion}")
            project.dependencies.add("implementation", "com.ing.zknotary:generator:${extension.generatorVersion}")
            project.dependencies.add("ksp", "com.ing.zknotary:generator:${extension.generatorVersion}")
        }

        // Only mark source as generated if the Idea plugin is loaded for the project
        project.plugins.withType(IdeaPlugin::class.java) {
            it as IdeaPlugin
            it.model.module.generatedSourceDirs =
                it.model.module.generatedSourceDirs + project.file(generatedSourcePath) + project.file(
                generatedTestSourcePath
            )
        }

        project.tasks.create("copyZincPlatformSources") { task ->
            task.doLast {
                project.copy { copy ->
                    copy.into(project.buildDir)
                    val file = project.configurations.findByName("zinc")
                        ?.files?.find { it.name.contains("zinc-platform-sources-${extension.zincPlatformSourcesVersion}") }
                    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                    copy.from(project.zipTree(file).matching { it.include("zinc-platform-source/**/*.zn") })
                }
            }
        }
    }
}
