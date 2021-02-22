package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.task.CompileZincTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

@Suppress("unused")
class ZKNotaryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("zkp", ZKNotaryExtension::class.java)

        project.plugins.withType(JavaPlugin::class.java) {
            // Add the required dependencies to consumer projects
            project.configurations.create("zinc")
            project.dependencies.add(
                "zinc",
                "com.ing.zknotary:zinc-platform-sources:${extension.zincPlatformSourcesVersion}"
            )
            project.dependencies.add("implementation", "com.ing.zknotary:notary:${extension.notaryVersion}")
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
        // Create task for building circuit
        project.tasks.create("compileZinc", CompileZincTask::class.java)
    }
}
