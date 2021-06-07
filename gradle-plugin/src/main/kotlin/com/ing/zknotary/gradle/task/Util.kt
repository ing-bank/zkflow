package com.ing.zknotary.gradle.task

import java.io.File

fun joinConstFiles(circuitPath: File, platformSourcesPath: File): String {
    return circuitPath.resolve("consts.zn").readText() + "\n\n" +
        platformSourcesPath.resolve("platform_consts.zn").readText()
}

fun String.folderIfExists(basePath: File) = takeIf { File(basePath.path + "/$this").exists() }
