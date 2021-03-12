package com.ing.zknotary.gradle.util

import java.io.File

/**
 * Do not include debug statements into the compilation code
 **/
fun removeDebugCode(targetFilePath: String) {
    File("$targetFilePath/").walk().forEach { file ->
        // Skip directories
        if (file.isFile && file.extension == "zn") {
            val lines = file.readLines()
            file.delete()
            file.createNewFile()

            lines.filter { s: String -> !s.contains("dbg!") }.map {
                file.appendText("$it\n")
            }
        }
    }
}
