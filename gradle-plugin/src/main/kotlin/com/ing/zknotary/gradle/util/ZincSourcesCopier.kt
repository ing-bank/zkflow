package com.ing.zknotary.gradle.util

import java.io.File
import java.nio.ByteBuffer

class ZincSourcesCopier(private val outputPath: File) {

    fun copyZincCircuitSources(circuitSources: File, circuitName: String, version: String) {
        circuitSources.copyRecursively(
            createOutputFile(outputPath),
            overwrite = true
        )

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

    fun copyZincPlatformSources(platformSources: Array<File>?) {
        platformSources?.forEach { file ->
            file.copyTo(
                createOutputFile(outputPath).resolve(file.name),
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
