package com.ing.zknotary.gradle.extension

import com.ing.zknotary.common.zkp.CircuitMetaData
import com.ing.zknotary.gradle.zinc.template.TemplateConfigurations
import com.ing.zknotary.gradle.zinc.template.TemplateParameters
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import java.io.File

open class ZKNotaryExtension(project: Project) : TemplateConfigurations() {

    companion object {
        const val NAME = "zkp"

        const val CONFIG_CIRCUIT_FILE = CircuitMetaData.CONFIG_CIRCUIT_FILE

        private const val MERGED_CIRCUIT_BUILD_PATH = "zinc"
        private const val CIRCUIT_SOURCES_BASE_PATH = "src/main/zinc"

        const val SKELETON_STATE_PATH = "skeleton-states"
        const val SKELETON_CIRCUIT_PATH = "skeleton-circuit"
    }

    @Input
    override val templateParameters = mutableListOf<TemplateParameters>()

    @Input
    var zincPlatformSourcesVersion: String? = "1.0-SNAPSHOT"

    @Input
    var notaryVersion: String? = "1.0-SNAPSHOT"

    @OutputDirectory
    val mergedCircuitOutputPath: File = project.buildDir.resolve(MERGED_CIRCUIT_BUILD_PATH)

    @InputDirectory
    val circuitSourcesBasePath: File = project.projectDir.resolve(CIRCUIT_SOURCES_BASE_PATH)

    @OutputDirectory
    val generatedTestResourcesDir: File = project.buildDir.resolve("resources/test")

    @Input
    var zincCommonFolderName = "common"

    @Input
    val zincFilesGlob = "**/*.zn"

    @Input
    val configFiles = "**/*.json"

    @Input
    val platformSourcesPath = "zinc-platform-sources/"

    @Input
    val platformLibrariesPath = "zinc-platform-libraries/"

    @Input
    val platformTemplatesPath = "zinc-platform-templates/"

    @Input
    val platformSamplesPath = "zinc-platform-samples/"

    @Input
    val statesSourcesPath = "states"

    @Input
    val merkleTemplate = "merkle_template.zn"

    @Input
    val mainTemplate = "main_template.zn"
}
