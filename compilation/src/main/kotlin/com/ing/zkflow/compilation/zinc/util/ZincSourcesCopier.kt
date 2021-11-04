package com.ing.zkflow.compilation.zinc.util

import java.io.File
import java.nio.ByteBuffer

public class ZincSourcesCopier(private val outputPath: File) {
    public fun copyZincCircuitSources(circuitSources: File, circuitName: String, version: String) {
        circuitSources
            .listFiles()
            ?.forEach { file ->
                val copy = outputPath.resolve(file.name)
                file.copyTo(copy, overwrite = true)
            }

        createOutputFile(outputPath.parentFile.resolve("Zargo.toml")).apply {
            if (!exists()) createNewFile()
            outputStream().channel
                .truncate(0)
                .write(
                    ByteBuffer.wrap(
                        """
    [circuit]
    name = "$circuitName"
    version = "$version"                                
                        """.trimIndent().toByteArray()
                    )
                )
        }
    }

    public fun copyZincCircuitStates(circuitStates: List<File>) {
        circuitStates.forEach { file ->
            file.copyTo(
                createOutputFile(outputPath).resolve(file.name),
                overwrite = true
            )
        }
    }

    public fun copyZincPlatformSources(platformSources: Array<File>?) {
        platformSources?.forEach { file ->
            file.copyTo(
                createOutputFile(outputPath).resolve(file.name),
                overwrite = true
            )
        }
    }

    public fun copyZincCircuitSourcesForTests(circuitSources: File) {
        circuitSources.listFiles { _, name -> name != "main.zn" }?.forEach {
            it.copyTo(
                createOutputFile(outputPath).resolve(it.name),
                overwrite = true
            )
        }
    }

    private fun createOutputFile(targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        targetFile.delete()
        targetFile.createNewFile()
        return targetFile
    }
}
