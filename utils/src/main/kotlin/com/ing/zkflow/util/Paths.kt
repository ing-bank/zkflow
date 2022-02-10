package com.ing.zkflow.util

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

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

/**
 * Run [command] in [this] directory, abort after [timeoutInSeconds].
 * Captures and returns stdout and stderr as strings.
 * @receiver The working directory
 * @param command The command to execute
 * @param timeoutInSeconds The timeout in seconds
 * @return Pair with stdout and stderr
 */
@Suppress("SpreadOperator", "MagicNumber")
@SuppressFBWarnings("COMMAND_INJECTION", "PATH_TRAVERSAL_IN", justification = "This is the whole point of this utility")
fun Path.runCommand(command: String, timeoutInSeconds: Long = 5): Pair<String, String> {
    val workingDir: File = toFile()
    val stdout = workingDir.resolve("stdout")
    val stderr = workingDir.resolve("stderr")

    val processBuilder: ProcessBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.to(stdout))
        .redirectError(ProcessBuilder.Redirect.to(stderr))

    if (!processBuilder.start()
        .waitFor(timeoutInSeconds, TimeUnit.SECONDS)
    ) {
        throw InterruptedCommandException(command, timeoutInSeconds)
    }

    return Pair(
        FileReader(stdout.path).readText(),
        FileReader(stderr.path).readText()
    )
}

class InterruptedCommandException(command: String, timeoutInSeconds: Long) : RuntimeException("Command '$command' was interrupted after $timeoutInSeconds seconds")
