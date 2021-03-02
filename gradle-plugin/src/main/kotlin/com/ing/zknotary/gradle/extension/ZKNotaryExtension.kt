package com.ing.zknotary.gradle.extension

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import java.io.File

open class ZKNotaryExtension(project: Project) {

    companion object {
        const val NAME = "zkp"

        private const val MERGED_CIRCUIT_BUILD_PATH = "zinc"
        private const val CIRCUIT_SOURCES_BASE_PATH = "src/main/zinc"
    }

    @Input
    var zincPlatformSourcesVersion: String? = "0.1.4-SNAPSHOT"

    @Input
    var notaryVersion: String? = "0.1.4-SNAPSHOT"

    @Input
    var generatorVersion: String? = "0.1.4-SNAPSHOT"

    @OutputDirectory
    val mergedCircuitOutputPath: File = project.buildDir.resolve(MERGED_CIRCUIT_BUILD_PATH)

    @InputDirectory
    val circuitSourcesBasePath: File = project.projectDir.resolve(CIRCUIT_SOURCES_BASE_PATH)
}
