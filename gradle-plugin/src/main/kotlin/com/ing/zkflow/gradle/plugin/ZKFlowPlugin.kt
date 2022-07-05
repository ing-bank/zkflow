package com.ing.zkflow.gradle.plugin

import com.ing.zkflow.gradle.task.GenerateZincCircuitsTask
import com.ing.zkflow.gradle.task.GenerateZkpStructureTask
import com.ing.zkflow.gradle.task.VerifyZkpStructureTask
import com.ing.zkflow.zinc.poet.generate.structure.GENERATE_ZKP_STRUCTURE
import com.ing.zkflow.zinc.poet.generate.structure.VERIFY_ZKP_STRUCTURE
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import java.net.URI

@Suppress("unused")
@SuppressFBWarnings("HARD_CODE_PASSWORD", justification = "False positive")
class ZKFlowPlugin : Plugin<Project> {
    @Suppress("LongMethod")
    override fun apply(project: Project) {
        project.repositories.apply {
            // For kotlinx.serialization plugin
            maven { it.url = URI.create("https://plugins.gradle.org/m2/") }
        }

        project.plugins.withType(JavaPlugin::class.java) {
            // Add the required dependencies to consumer projects
            project.dependencies.add(IMPLEMENTATION, zkflowArtifact("protocol"))
            // TODO zinc-code-generation is only needed at compile time, by the gradlePlugin, so compileOnly sounds better...
            project.dependencies.add(IMPLEMENTATION, zkflowArtifact("zinc-code-generation"))

            project.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            // KSP.
            project.pluginManager.apply("com.google.devtools.ksp")
            project.dependencies.add("ksp", zkflowArtifact("compiler-plugin-ksp"))
        }

        val generateZincCircuitsTask = project.tasks.create(
            GENERATE_ZINC_CIRCUITS,
            GenerateZincCircuitsTask::class.java
        )
        generateZincCircuitsTask
            .dependsOn(COMPILE_KOTLIN) // So the command metadata can be found
            .mustRunAfter(COMPILE_KOTLIN)

        val verifyZkpStructureTask = project.tasks.create(
            VERIFY_ZKP_STRUCTURE,
            VerifyZkpStructureTask::class.java
        )
        verifyZkpStructureTask
            .dependsOn(COMPILE_KOTLIN)
            .mustRunAfter(COMPILE_KOTLIN)

        val generateZkpStructureTask = project.tasks.create(
            GENERATE_ZKP_STRUCTURE,
            GenerateZkpStructureTask::class.java
        )
        generateZkpStructureTask
            .dependsOn(VERIFY_ZKP_STRUCTURE)
            .mustRunAfter(VERIFY_ZKP_STRUCTURE)

        project.tasks.getByPath(COMPILE_KOTLIN).finalizedBy(VERIFY_ZKP_STRUCTURE)
        project.tasks.getByPath(VERIFY_ZKP_STRUCTURE).finalizedBy(GENERATE_ZINC_CIRCUITS)
        project.tasks.getByPath(ASSEMBLE).dependsOn(GENERATE_ZINC_CIRCUITS)
    }

    companion object {
        const val ASSEMBLE = "assemble"
        const val COMPILE_KOTLIN = "compileKotlin"
        const val IMPLEMENTATION = "implementation"
        const val GENERATE_ZINC_CIRCUITS = "generateZincCircuits"
        const val ZKFLOW_GROUP = "com.ing.zkflow"
        private fun zkflowArtifact(artifact: String, version: String? = "1.0-SNAPSHOT") = "$ZKFLOW_GROUP:$artifact:$version"
    }
}
