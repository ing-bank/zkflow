package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.plugin.circuitNames
import com.ing.zknotary.gradle.plugin.zincCommonFolderName
import com.ing.zknotary.gradle.plugin.zincTestFolderNames
import com.ing.zknotary.gradle.plugin.zkNotaryExtension
import com.ing.zknotary.gradle.zinc.util.ZincSourcesCopier
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CopyZincCircuitSourcesForTestsTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkNotaryExtension
        val commonFolderName = project.zincCommonFolderName

        project.zincTestFolderNames.forEach { testFolder ->
            val copier = ZincSourcesCopier(testFolder.resolve("src"))
            val circuitSources: List<File> = listOfNotNull(
                commonFolderName?.let { listOf(extension.circuitSourcesBasePath.resolve(it)) },
                project.circuitNames?.map { extension.mergedCircuitOutputPath.resolve(it).resolve("src") },
            ).flatten()

            circuitSources.forEach {
                copier.copyZincCircuitSourcesForTests(it)
            }
        }
    }
}
