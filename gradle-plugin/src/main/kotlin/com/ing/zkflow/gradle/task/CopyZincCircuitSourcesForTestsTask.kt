package com.ing.zkflow.gradle.task

import com.ing.zkflow.compilation.zinc.util.ZincSourcesCopier
import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.zincCommonFolderName
import com.ing.zkflow.gradle.plugin.zincTestFolderNames
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CopyZincCircuitSourcesForTestsTask : DefaultTask() {

    @TaskAction
    fun createCopyZincSources() {
        val extension = project.zkFlowExtension
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
