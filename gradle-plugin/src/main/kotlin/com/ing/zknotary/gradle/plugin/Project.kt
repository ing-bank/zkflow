package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import com.ing.zknotary.gradle.task.folderIfExists
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import java.io.File

val Project.platformSourcesFileTree: FileTree
    get() {
        return configurations.getByName("zinc")
            .files
            .single { it.name.contains("zinc-platform-sources-${zkNotaryExtension.zincPlatformSourcesVersion}") }
            .let { project.zipTree(it) }
    }

val Project.zkNotaryExtension: ZKNotaryExtension
    get() {
        return extensions.findByType(ZKNotaryExtension::class.java)
            ?: error("ZKNotaryExtension was not loaded")
    }

val Project.circuitNames: List<String>?
    get() {
        return zkNotaryExtension.circuitSourcesBasePath
            .listFiles { file, _ -> file?.isDirectory ?: false }?.map { it.name }
            ?.filterNot { it == zkNotaryExtension.zincCommonFolderName }
            ?.filterNot { it.contains(zkNotaryExtension.statesSourcesPath) }
    }

val Project.zincCommonFolderName: String?
    get() {
        return zkNotaryExtension.zincCommonFolderName.folderIfExists(zkNotaryExtension.circuitSourcesBasePath)
    }

val Project.platformSources: Array<File>
    get() {
        return project.platformSourcesFileTree.matching { it.include(zkNotaryExtension.platformSourcesPath + zkNotaryExtension.zincFilesGlob) }
            .toList().toTypedArray()
    }

val Project.platformSourcesRootPath: File
    get() {
        return project.platformSources.sortedArray().firstOrNull()?.parentFile?.absoluteFile
            ?: error("Platform Sources are empty")
    }

val Project.platformLibraries: Array<File>
    get() {
        return project.platformSourcesFileTree.matching { it.include(zkNotaryExtension.platformLibrariesPath + zkNotaryExtension.zincFilesGlob) }
            .toList().toTypedArray()
    }

val Project.platformTemplates: Array<File>
    get() {
        return project.platformSourcesFileTree.matching { it.include(zkNotaryExtension.platformTemplatesPath + zkNotaryExtension.zincFilesGlob) }
            .toList().toTypedArray()
    }

val Project.platformSamples: FileTree
    get() {
        return project.platformSourcesFileTree.matching {
            it.include(zkNotaryExtension.platformSamplesPath + zkNotaryExtension.zincFilesGlob)
                .include(zkNotaryExtension.platformSamplesPath + zkNotaryExtension.configFiles)
        }
    }

// It is assumed that for every zinc test a similarly named folder ending in 'Test' (e.g. <test-name>Test) exists within
// the resources folder of the project, containing the necessary zinc files for the test circuit to be run ('Zargo.toml',
// 'src/main.zn')
val Project.zincTestFolderNames: List<File>
    get() {
        return zkNotaryExtension.generatedTestResourcesDir.walkTopDown().filter { it.name.endsWith("Test") }.toList()
    }

fun Project.getTemplateContents(templateFileName: String): String {
    return project.platformTemplates.single { it.name == templateFileName }.readText()
}
