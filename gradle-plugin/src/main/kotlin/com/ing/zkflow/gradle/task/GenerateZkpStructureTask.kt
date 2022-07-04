package com.ing.zkflow.gradle.task

import com.ing.zkflow.zinc.poet.generate.structure.ZKP_STRUCTURE_JSON
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class GenerateZkpStructureTask : DefaultTask() {
    // NOTE: Must be public for the @InputDirectory to be recognized
    @InputDirectory
    @Suppress("unused") // specify src folder as inputs to gradle
    val sourceInputs: ConfigurableFileTree = project.fileTree("src")

    // NOTE: Must be public for the @InputDirectory to be recognized
    @InputDirectory
    val generatedKspResources: File = project.buildDir.resolve("generated/ksp/main/resources")

    // NOTE: Must be public for the @OutputFile to be recognized
    @OutputFile
    @Suppress("unused") // specify structure.json as output to gradle
    val generatedStructure: File = project.projectDir.resolve(ZKP_STRUCTURE_JSON)

    @TaskAction
    fun generateZkpStructure() {
        project.javaexec {
            val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class.java)
            val main = javaPlugin.sourceSets.findByName("main") ?: error("Can't find main sourceSet")

            it.main = "com.ing.zkflow.zinc.poet.generate.structure.GenerateZkpStructureKt"
            // We must add 'build/generated/ksp/src/main/resources' to the main sourceSet if it exists, because
            // otherwise the generated META-INF/services file is not picked up by the `generateZkpStructure` task.
            // It would be the nicest if KSP already did this, however it doesn't.
            it.classpath = main.runtimeClasspath + project.files(generatedKspResources).filter(File::exists)
        }
    }
}
