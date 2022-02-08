package com.ing.zkflow.gradle.plugin

import com.ing.zkflow.gradle.extension.ZKFlowExtension
import org.gradle.api.Project

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
