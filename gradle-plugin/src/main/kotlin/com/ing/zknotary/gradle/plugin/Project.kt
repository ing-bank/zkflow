package com.ing.zknotary.gradle.plugin

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
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
    }

val Project.platformSources: Array<File>
    get() {
        return project.platformSourcesFileTree.matching { it.include(zkNotaryExtension.platformSourcesPath + zkNotaryExtension.zincFilesGlob) }
            .toList().toTypedArray()
    }

val Project.platformSourcesRootPath: File
    get() {
        return project.platformSources.sortedArray().firstOrNull()?.parentFile?.absoluteFile ?: error("Platform Sources are empty")
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
        return project.platformSourcesFileTree.matching { it.include(zkNotaryExtension.platformSamplesPath + zkNotaryExtension.zincFilesGlob) }
    }

fun Project.getTemplateContents(templateFileName: String): String {
    return project.platformTemplates.single { it.name == templateFileName }.readText()
}
