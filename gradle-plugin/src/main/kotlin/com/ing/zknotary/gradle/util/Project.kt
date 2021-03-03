package com.ing.zknotary.gradle.util

import com.ing.zknotary.gradle.extension.ZKNotaryExtension
import org.gradle.api.Project
import org.gradle.api.file.FileTree

val Project.platformSources: FileTree
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
