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

        // Load the KSP plugin for consumers
        project.plugins.apply("symbol-processing")

        // Add the required dependencies to consumer projects
        project.dependencies.add("implementation", "com.ing.zknotary:notary:${extension.notaryVersion}")
        project.dependencies.add("implementation", "com.ing.zknotary:generator:${extension.generatorVersion}")
        project.dependencies.add("ksp", "com.ing.zknotary:generator:${extension.generatorVersion}")

        // For each consumer project, mark the KSP generated code as source
        project.allprojects { subProject ->
            val generatedSourcePath = "${subProject.buildDir.name}/generated/ksp/main/kotlin"
            val generatedTestSourcePath = "${subProject.buildDir.name}/generated/ksp/test/kotlin"

            val sourceSets = subProject.properties["sourceSets"] as SourceSetContainer?

            // Only add to the sourcesets if the Java plugin is loaded for the project
            subProject.plugins.withType(JavaPlugin::class.java) {
                sourceSets!!.getByName("main").java.srcDir(generatedSourcePath)
                sourceSets.getByName("test").java.srcDir(generatedTestSourcePath)
            }

            // Only mark source as generated if the Idea plugin is loaded for the project
            subProject.plugins.withType(IdeaPlugin::class.java) {
                it as IdeaPlugin
                it.model.module.generatedSourceDirs =
                    it.model.module.generatedSourceDirs + subProject.file(generatedSourcePath) + subProject.file(generatedTestSourcePath)
            }
        }
    }
}