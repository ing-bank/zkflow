package com.ing.zkflow.gradle.plugin

import com.ing.zkflow.gradle.extension.ZKFlowExtension
import com.ing.zkflow.gradle.task.GenerateZincCircuitsTask
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import java.net.URI

@Suppress("unused")
@SuppressFBWarnings("HARD_CODE_PASSWORD", justification = "False positive")
class ZKFlowPlugin : Plugin<Project> {
    @Suppress("LongMethod")
    override fun apply(project: Project) {
        val extension = project.extensions.create(ZKFlowExtension.NAME, ZKFlowExtension::class.java, project)

        project.repositories.apply {
            // For kotlinx.serialization plugin
            maven { it.url = URI.create("https://plugins.gradle.org/m2/") }

            // For arrow
            maven { it.url = URI.create("https://oss.sonatype.org/content/repositories/snapshots/") }
            // For Arrow to enable bespoke treatment of the Corda classes.
            maven { it.url = URI.create("https://software.r3.com/artifactory/corda") }

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
            project.dependencies.add(IMPLEMENTATION, extension.zkflow("protocol"))
            project.dependencies.add(IMPLEMENTATION, extension.zkflow("annotations"))
            // TODO zinc-code-generation is only needed at compile time, by the gradlePlugin, so compileOnly sounds better...
            project.dependencies.add(IMPLEMENTATION, extension.zkflow("zinc-code-generation"))

            project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            // KSP.
            project.pluginManager.apply("com.google.devtools.ksp")
            project.dependencies.add("ksp", extension.zkflow("compiler-plugin-ksp"))

            // Arrow.
            applyArrowCompilerPlugin(project, extension)
        }

        val generateZincCircuitsTask = project.tasks.create(GENERATE_ZINC_CIRCUITS, GenerateZincCircuitsTask::class.java)
        generateZincCircuitsTask
            .dependsOn(COMPILE_KOTLIN) // So the command metadata can be found
            .mustRunAfter(COMPILE_KOTLIN)

        project.tasks.getByPath(ASSEMBLE).dependsOn(GENERATE_ZINC_CIRCUITS)
    }

    private fun applyArrowCompilerPlugin(
        project: Project,
        extension: ZKFlowExtension
    ) {
        val arrowCompilerPluginConfiguration: Configuration = project.configurations.create(ARROW_COMPILER_PLUGIN)
        project.dependencies.add(ARROW_COMPILER_PLUGIN, extension.zkflow("compiler-plugin-arrow"))
        project.dependencies.add(IMPLEMENTATION, extension.zkflow("serialization-candidate"))

        project
            .tasks
            .filterIsInstance<KotlinCompile<*>>()
            .forEach { task ->
                task.logger.quiet("[${task.name}] Modifying free compiler arguments to apply compiler-plugin-arrow-${extension.notaryVersion}.jar")

                val toInclude = listOf(
                    Pair(ZKFLOW_GROUP, "utils"),
                    Pair(ZKFLOW_GROUP, "annotations"),
                    Pair(ZKFLOW_GROUP, "serialization-candidate"),
                    Pair("org.jetbrains.kotlinx", "kotlinx-serialization-core-jvm"),
                    Pair("io.arrow-kt", "arrow-meta"),
                )

                val resolvedArtifacts = arrowCompilerPluginConfiguration.resolvedConfiguration.resolvedArtifacts

                resolvedArtifacts.filter { artifact ->
                    toInclude.any {
                        artifact.moduleVersion.id.group == it.first && artifact.moduleVersion.id.name == it.second
                    }
                }.forEach {
                    task.logger.quiet("[${task.name}] Adding dependency: ${it.id.displayName}")
                    project.dependencies.add("kotlinCompilerPluginClasspath", project.files(it.file.toURI()))
                }

                val compilerPluginArrow =
                    resolvedArtifacts.single { artifact ->
                        artifact.moduleVersion.id.group == ZKFLOW_GROUP && artifact.moduleVersion.id.name == "compiler-plugin-arrow"
                    }

                task.kotlinOptions.freeCompilerArgs += listOf("-Xplugin=${compilerPluginArrow.file.absolutePath}")

                task.logger.quiet(
                    "[${task.name}] Arrow compiler plugin application:${
                    task.kotlinOptions.freeCompilerArgs
                        .joinToString(separator = "\n\t") { it }
                        .let { if (it.isNotBlank()) "\n\t$it" else it }
                    }"
                )
            }
    }

    companion object {
        const val ARROW_COMPILER_PLUGIN = "arrowCompilerPlugin"
        const val ASSEMBLE = "assemble"
        const val COMPILE_KOTLIN = "compileKotlin"
        const val IMPLEMENTATION = "implementation"
        const val GENERATE_ZINC_CIRCUITS = "generateZincCircuits"
        const val ZKFLOW_GROUP = "com.ing.zkflow"

        private fun ZKFlowExtension.zkflow(artifact: String, version: String? = this.notaryVersion) = "$ZKFLOW_GROUP:$artifact:$version"
    }
}
