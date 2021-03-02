package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.task.CompileZincTask
import com.ing.zknotary.gradle.task.CreateZincFilesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.ide.idea.IdeaPlugin
import java.io.File
import java.nio.ByteBuffer

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

        project.tasks.create("copyZincPlatformSources") { task ->
            task.doLast {
                val circuitDirs = extension.circuitSourcesBasePath.listFiles { file: File?, name: String? ->
                    file?.isDirectory ?: false
                }?.map { it.name }

                circuitDirs?.forEach { circuitName ->
                    project.copy { copy ->
                        copy.into(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
                        project.configurations.findByName("zinc")
                            ?.files
                            ?.find { it.name.contains("zinc-platform-sources-${extension.zincPlatformSourcesVersion}") }
                            ?.let { file ->
                                copy.from(
                                    project.zipTree(file).matching {
                                        it.include("**/*.zn")
                                        it.exclude("**/META-INF")
                                    }
                                )
                            }
                    }
                }
            }
        }
        project.tasks.create("copyZincCircuitSources") { task ->
            task.doLast {
                val circuitDirs = extension.circuitSourcesBasePath.listFiles { file: File?, name: String? ->
                    file?.isDirectory ?: false
                }?.map { it.name }

                circuitDirs?.forEach { circuitName ->
                    project.copy { copy ->
                        copy.from(extension.circuitSourcesBasePath.resolve(circuitName))
                        copy.into(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
                    }
                    // TODO: Create Zargo.toml file
                    extension.mergedCircuitOutputPath.resolve(circuitName).resolve("Zargo.toml").apply {
                        if (!exists()) createNewFile()
                        outputStream().channel
                            .truncate(0)
                            .write(
                                ByteBuffer.wrap(
                                    """
[circuit]
name = "$circuitName"
version = "${project.version}"                                
                                    """.trimIndent().toByteArray()
                                )
                            )
                    }
                }
            }
        }

        // Create task for creating custom zinc files on zkdapp
        project.tasks.create("createZincFiles", CreateZincFilesTask::class.java)

        // Create task for building circuit
        project.tasks.create("compileZinc", CompileZincTask::class.java)
    }
}
