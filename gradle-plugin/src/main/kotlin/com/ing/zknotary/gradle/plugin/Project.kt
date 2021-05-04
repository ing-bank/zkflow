package com.ing.zknotary.gradle.zinc.util

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import java.io.File

val Project.platformSourcesDirectory: FileTree
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
        return project.platformSourcesDirectory.matching { it.include(zkNotaryExtension.platformSourcesPath) }.toList().toTypedArray()
    }

val Project.platformLibraries: Array<File>
    get() {
        return project.platformSourcesDirectory.matching { it.include(zkNotaryExtension.platformLibrariesPath) }.toList().toTypedArray()
    }

val Project.platformTemplates: Array<File>
    get() {
        return project.platformSourcesDirectory.matching { it.include(zkNotaryExtension.platformTemplatesPath) }.toList().toTypedArray()
    }

val Project.platformSamples: FileTree
    get() {
        return project.platformSourcesDirectory.matching { it.include(zkNotaryExtension.platformSamplesPath) }
    }

fun Project.getTemplateContents(templateFileName: String): String {
    return project.platformTemplates.single { it.name.contains(templateFileName) }.readText()
}
