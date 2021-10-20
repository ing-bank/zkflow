package com.ing.zkflow.gradle.plugin

import com.ing.zkflow.compilation.folderIfExists
import com.ing.zkflow.gradle.extension.ZKFlowExtension
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import java.io.File

val Project.platformSourcesFileTree: FileTree
    get() {
        return configurations.getByName("zinc")
            .files
            .single { it.name.contains("zinc-platform-sources-${zkFlowExtension.zincPlatformSourcesVersion}") }
            .let { project.zipTree(it) }
    }

val Project.zkFlowExtension: ZKFlowExtension
    get() {
        return extensions.findByType(ZKFlowExtension::class.java)
            ?: error("ZKFlowExtension was not loaded")
    }

val Project.circuitNames: List<String>?
    get() {
        return zkFlowExtension.circuitSourcesBasePath
            .listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }
            ?.filterNot { it == zkFlowExtension.zincCommonFolderName }
            ?.filterNot { it == zkFlowExtension.statesSourcesPath }
    }

val Project.zincCommonFolderName: String?
    get() {
        return zkFlowExtension.zincCommonFolderName.folderIfExists(zkFlowExtension.circuitSourcesBasePath)
    }

val Project.zincStatesFolderName: String?
    get() {
        return zkFlowExtension.statesSourcesPath.folderIfExists(zkFlowExtension.circuitSourcesBasePath)
    }

val Project.platformSources: Array<File>
    get() {
        return project.platformSourcesFileTree.matching { it.include(zkFlowExtension.platformSourcesPath + zkFlowExtension.zincFilesGlob) }
            .toList().toTypedArray()
    }

val Project.platformSourcesRootPath: File
    get() {
        return project.platformSources.sortedArray().firstOrNull()?.parentFile?.absoluteFile
            ?: error("Platform Sources are empty")
    }

val Project.platformLibraries: Array<File>
    get() {
        return project.platformSourcesFileTree.matching { it.include(zkFlowExtension.platformLibrariesPath + zkFlowExtension.zincFilesGlob) }
            .toList().toTypedArray()
    }

val Project.platformTemplates: Array<File>
    get() {
        return project.platformSourcesFileTree.matching { it.include(zkFlowExtension.platformTemplatesPath + zkFlowExtension.zincFilesGlob) }
            .toList().toTypedArray()
    }

val Project.platformSkeletonState: FileTree
    get() = project.platformSourcesFileTree.matching {
        it
            .include(
                zkFlowExtension.platformSamplesPath +
                    ZKFlowExtension.SKELETON_STATE_PATH +
                    zkFlowExtension.zincFilesGlob
            )
    }

val Project.platformSkeletonCircuit: FileTree
    get() = project.platformSourcesFileTree.matching {
        it
            .include(
                zkFlowExtension.platformSamplesPath +
                    ZKFlowExtension.SKELETON_CIRCUIT_PATH +
                    zkFlowExtension.zincFilesGlob
            )
            .include(
                zkFlowExtension.platformSamplesPath +
                    ZKFlowExtension.SKELETON_CIRCUIT_PATH +
                    zkFlowExtension.configFiles
            )
    }

// It is assumed that for every zinc test a similarly named folder ending in 'Test' (e.g. <test-name>Test) exists within
// the resources folder of the project, containing the necessary zinc files for the test circuit to be run ('Zargo.toml',
// 'src/main.zn')
val Project.zincTestFolderNames: List<File>
    get() {
        return zkFlowExtension.generatedTestResourcesDir.walkTopDown().filter { it.name.endsWith("Test") }.toList()
    }

fun Project.getTemplateContents(templateFileName: String): String {
    return project.platformTemplates.single { it.name == templateFileName }.readText()
}
