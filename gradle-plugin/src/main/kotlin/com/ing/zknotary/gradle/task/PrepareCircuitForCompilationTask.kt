package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.plugin.circuitNames
import com.ing.zknotary.gradle.plugin.platformSourcesRootPath
import com.ing.zknotary.gradle.plugin.zkNotaryExtension
import com.ing.zknotary.gradle.zinc.util.MerkleReplacer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class PrepareCircuitForCompilationTask : DefaultTask() {

    @TaskAction
    fun prepareCircuitForCompilation() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val replacer = MerkleReplacer(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))

            val consts =
                joinConstFiles(extension.circuitSourcesBasePath.resolve(circuitName), project.platformSourcesRootPath)

            replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
            replacer.setCorrespondingMerkleTreeFunctionForMainTree()
        }
    }
}
