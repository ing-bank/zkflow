package com.ing.zkflow.gradle.extension

import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.CIRCUIT_SOURCES_BASE_PATH
import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.CIRCUIT_SOURCES_STATES_PATH
import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.MERGED_CIRCUIT_BUILD_PATH
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import java.io.File

open class ZKFlowExtension(project: Project) {

    companion object {
        const val NAME = "zkp"
    }

    @Input
    var zincPlatformSourcesVersion: String? = "1.0-SNAPSHOT"

    @Input
    var notaryVersion: String? = "1.0-SNAPSHOT"

    val mergedCircuitOutputPath: File = project.buildDir.resolve(MERGED_CIRCUIT_BUILD_PATH)

    val circuitSourcesBasePath: File = project.projectDir.resolve(CIRCUIT_SOURCES_BASE_PATH)

    val generatedTestResourcesDir: File = project.buildDir.resolve("resources/test")

    @Input
    var zincCommonFolderName = "common"

    @Input
    val statesSourcesPath = CIRCUIT_SOURCES_STATES_PATH
}
