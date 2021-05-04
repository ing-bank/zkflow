package com.ing.zknotary.gradle.zinc.util

import java.io.File
import java.nio.file.Path

fun createOutputFile(targetFile: File): File {
    targetFile.parentFile?.mkdirs()
    targetFile.delete()
    targetFile.createNewFile()
    return targetFile
}

fun createOutputFile(path: Path): File = createOutputFile(path.toFile())
