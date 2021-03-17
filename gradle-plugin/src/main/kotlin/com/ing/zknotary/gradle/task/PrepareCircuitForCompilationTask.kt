package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.Merkle
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.removeDebugCode
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class PrepareCircuitForCompilationTask : DefaultTask() {

    @TaskAction
    fun prepareCircuitForCompilation() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val merkle = Merkle(circuitName, extension.mergedCircuitOutputPath, extension.circuitSourcesBasePath)

            merkle.setCorrespondingMerkleTreeFunctionForComponentGroups()
            merkle.setCorrespondingMerkleTreeFunctionForMainTree()

            removeDebugCode(circuitName, extension.mergedCircuitOutputPath)
        }
    }
}
