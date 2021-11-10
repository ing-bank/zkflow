package com.ing.zkflow.util

import java.nio.file.Files
import java.nio.file.Path

/**
 * Verify whether [fileName] is a regular file at the given [Path], otherwise it is created.
 * @receiver The base directory
 * @param fileName The name of the required file
 * @throws IllegalArgumentException When [fileName] exists, but is not a regular file.
 * @return [Path] for [fileName]
 */
fun Path.ensureFile(fileName: String): Path {
    val file = resolve(fileName)
    if (Files.notExists(file)) {
        Files.createFile(file)
    } else if (!Files.isRegularFile(file)) {
        throw IllegalArgumentException("Not a regular file: $file")
    }
    return file
}

/**
 * Verify whether [directoryName] is a directory at the given [Path], otherwise it is created.
 * @receiver The base directory
 * @param directoryName The name of the required directory
 * @throws IllegalArgumentException When [directoryName] exists, but is not a directory.
 * @return [Path] for [directoryName]
 */
fun Path.ensureDirectory(directoryName: String): Path {
    val directory = resolve(directoryName)
    if (Files.notExists(directory)) {
        Files.createDirectory(directory)
    } else if (!Files.isDirectory(directory)) {
        throw IllegalArgumentException("Not a directory: $directory")
    }
    return directory
}
