package com.ing.zkflow.gradle.task

import com.ing.zkflow.zinc.poet.generate.structure.ZKP_STRUCTURE_JSON
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class VerifyZkpStructureTask : DefaultTask() {
    // NOTE: Must be public for the @InputDirectory to be recognized
    @InputDirectory
    @Suppress("unused")
    val sourceInputs = project.fileTree("src")

    // NOTE: Must be public for the @OutputDirectory to be recognized
    @InputDirectory
    val generatedKspResources: File = project.buildDir.resolve("generated/ksp/main/resources")

    // NOTE: Must be public for the @OutputDirectory to be recognized
    @InputFile
    val generatedStructure: File = project.projectDir.resolve(ZKP_STRUCTURE_JSON)

    @TaskAction
    @Suppress("SpreadOperator")
    fun generateZincCircuits() {
        project.javaexec {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            val main = javaPlugin.sourceSets.findByName("main") ?: error("Can't find main sourceSet")

            it.main = "com.ing.zkflow.zinc.poet.generate.structure.VerifyZkpStructureKt"
            it.classpath = main.runtimeClasspath + project.files(generatedKspResources).filter(File::exists)
        }
    }
}
