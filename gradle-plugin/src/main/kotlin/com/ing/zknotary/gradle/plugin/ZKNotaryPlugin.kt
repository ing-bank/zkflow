package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.task.CompileZincTask
import com.ing.zknotary.gradle.task.CreateZincFilesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.RelativePath
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.ide.idea.IdeaPlugin
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

        createCopyZincPlatformSourcesTask(project)
        createCopyZincCircuitSourcesTask(project)
        createGenerateZincPlatformCodeFromTemplatesTask(project)

        // Create task for creating custom zinc files on zkdapp
        project.tasks.create("createZincFiles", CreateZincFilesTask::class.java)

        // Create task for building circuit
        project.tasks.create("compileZinc", CompileZincTask::class.java)
    }

    private fun createGenerateZincPlatformCodeFromTemplatesTask(project: Project) {
        project.tasks.create("generateZincPlatformCodeFromTemplates") { task ->
            task.doLast {
                val templateContents = project.getTemplateContents("floating_point.zn")

                val extension = project.zkNotaryExtension
                project.circuitNames?.forEach { circuitName ->
                    extension.bigDecimalSizes.forEach {
                        val floatingPointContent = templateContents.replace("\${INTEGER_SIZE_PLACEHOLDER}", it.first.toString())
                            .replace("\${FRACTION_SIZE_PLACEHOLDER}", it.second.toString())
                        val sizeSuffix = "${it.first}_${it.second}"
                        val targetFile = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src/floating_point_$sizeSuffix.zn")
                        targetFile.delete()
                        targetFile.createNewFile()
                        targetFile.writeBytes(floatingPointContent.toByteArray())
                    }
                }
            }
        }
    }

    private fun createCopyZincCircuitSourcesTask(project: Project) {
        project.tasks.create("copyZincCircuitSources") { task ->
            task.doLast {
                val extension = project.zkNotaryExtension
                project.circuitNames?.forEach { circuitName ->
                    project.copy { copy ->
                        copy.from(extension.circuitSourcesBasePath.resolve(circuitName))
                        copy.into(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
                    }

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
    }

    private fun createCopyZincPlatformSourcesTask(project: Project) {
        project.tasks.create("copyZincPlatformSources") { task ->
            task.doLast {
                val extension = project.zkNotaryExtension
                val sources = project.platformSources.matching { it.include("zinc-platform-sources/**/*.zn") }
                project.circuitNames?.forEach { circuitName ->
                    project.copy { copy ->
                        copy.into(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
                        copy.from(sources).eachFile {
                            @Suppress("SpreadOperator")
                            it.relativePath = RelativePath(true, *it.relativePath.segments.drop(1).toTypedArray())
                        }
                        copy.includeEmptyDirs = false
                    }
                }
            }
        }
    }

    private fun Project.getTemplateContents(templateFileName: String): String {
        return project.platformSources.matching {
            it.include("zinc-platform-templates/$templateFileName")
        }.singleFile.readText()
    }

    private val Project.platformSources: FileTree
        get() {
            return configurations.getByName("zinc")
                .files
                .single { it.name.contains("zinc-platform-sources-${zkNotaryExtension.zincPlatformSourcesVersion}") }
                .let { project.zipTree(it) }
        }

    private val Project.circuitNames: List<String>?
        get() {
            return zkNotaryExtension.circuitSourcesBasePath
                .listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }
        }

    private val Project.zkNotaryExtension: ZKNotaryExtension
        get() {
            return extensions.findByType(ZKNotaryExtension::class.java)
                ?: error("ZKNotaryExtension was not loaded")
        }
}
