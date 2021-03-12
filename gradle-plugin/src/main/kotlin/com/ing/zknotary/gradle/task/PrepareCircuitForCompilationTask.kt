package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.removeDebugCode
import com.ing.zknotary.gradle.util.setCorrespondingMerkleTreeFunctionForComponentGroups
import com.ing.zknotary.gradle.util.setCorrespondingMerkleTreeFunctionForMainTree
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class PrepareCircuitForCompilationTask : DefaultTask() {

    @TaskAction
    fun prepareCircuitForCompilation() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val constsContent = File("${extension.circuitSourcesBasePath.resolve(circuitName)}/consts.zn").readText()
            val targetFilePath = extension.mergedCircuitOutputPath.resolve(circuitName).absolutePath

            removeDebugCode(targetFilePath)
            setCorrespondingMerkleTreeFunctionForComponentGroups(targetFilePath, constsContent)
            setCorrespondingMerkleTreeFunctionForMainTree(targetFilePath, constsContent)
        }
    }
}
