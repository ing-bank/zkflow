package com.ing.zkflow.gradle.extension

import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.CIRCUIT_SOURCES_BASE_PATH
import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.CIRCUIT_SOURCES_STATES_PATH
import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.MERGED_CIRCUIT_BUILD_PATH
import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.PLATFORM_SOURCES_PATH
import com.ing.zkflow.compilation.ZKFlowCompilationDefaults.PLATFORM_TEMPLATES_PATH
import com.ing.zkflow.compilation.zinc.template.TemplateConfigurations
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import java.io.File

open class ZKFlowExtension(project: Project) : TemplateConfigurations() {

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

    /**
     * The class where the GenerateZincPlatformCodeFromTemplatesJavaExecTask can find the template configuration
     */
    @Input
    var zkFlowTemplateConfigurationClassName: String? = null

    @Input
    var zincCommonFolderName = "common"

    @Input
    val zincFilesGlob = "**/*.zn"

    @Input
    val platformSourcesPath = PLATFORM_SOURCES_PATH

    @Input
    val platformLibrariesPath = "zinc-platform-libraries/"

    @Input
    val platformTemplatesPath = PLATFORM_TEMPLATES_PATH

    @Input
    val platformSamplesPath = "zinc-platform-samples/"

    @Input
    val statesSourcesPath = CIRCUIT_SOURCES_STATES_PATH

    @Input
    val merkleTemplate = "merkle_template.zn"

    @Input
    val mainTemplate = "main_template.zn"
}
