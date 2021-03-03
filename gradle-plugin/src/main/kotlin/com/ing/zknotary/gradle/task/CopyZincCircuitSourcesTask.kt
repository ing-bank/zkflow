package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.nio.ByteBuffer

abstract class CopyZincCircuitSourcesTask : DefaultTask() {

    @TaskAction
    fun createCopyZincCircuitSources() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            project.copy { copy ->
                copy.from(extension.circuitSourcesBasePath.resolve(circuitName))
                copy.into(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            }

            extension.mergedCircuitOutputPath.resolve(circuitName).resolve("Zargo.toml").apply {
                if (!exists()) createNewFile()
                outputStream().channel
                    .truncate(0)
                    .write(
                        ByteBuffer.wrap(
                            """
    [circuit]
    name = "$circuitName"
    version = "${project.version}"                                
                            """.trimIndent().toByteArray()
                        )
                    )
            }
        }
    }
}
