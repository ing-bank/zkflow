package com.ing.zknotary.gradle.util

import java.io.File
import java.nio.ByteBuffer

class Copy(private val circuitName: String, private val mergedCircuitOutput: File, private val circuitSourcesBase: File) {

    fun createCopyZincCircuitSources(version: String) {
        circuitSourcesBase.resolve(circuitName).copyRecursively(
            mergedCircuitOutput.resolve(circuitName).resolve("src"),
            overwrite = true
        )

        mergedCircuitOutput.resolve(circuitName).resolve("Zargo.toml").apply {
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

    fun createCopyZincPlatformSources(zincPlatformSources: Array<File>?) {
        zincPlatformSources?.forEach { file ->
            file?.copyRecursively(
                mergedCircuitOutput.resolve(circuitName).resolve("src").resolve(file.name),
                overwrite = true
            )
        }
    }
}
