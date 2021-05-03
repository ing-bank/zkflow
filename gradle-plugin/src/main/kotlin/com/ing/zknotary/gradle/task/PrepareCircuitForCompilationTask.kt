package com.ing.zknotary.gradle.task

import com.ing.zknotary.gradle.util.MerkleReplacer
import com.ing.zknotary.gradle.util.circuitNames
import com.ing.zknotary.gradle.util.zkNotaryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class PrepareCircuitForCompilationTask : DefaultTask() {

    @TaskAction
    fun prepareCircuitForCompilation() {
        val extension = project.zkNotaryExtension
        project.circuitNames?.forEach { circuitName ->
            val replacer = MerkleReplacer(extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src"))
            val consts = extension.circuitSourcesBasePath.resolve(circuitName).resolve("consts.zn").readText()

            replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
            replacer.setCorrespondingMerkleTreeFunctionForMainTree()
        }
    }
}
