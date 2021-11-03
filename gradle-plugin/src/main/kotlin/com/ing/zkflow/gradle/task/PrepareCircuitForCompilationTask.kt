package com.ing.zkflow.gradle.task

import com.ing.zkflow.compilation.joinConstFiles
import com.ing.zkflow.compilation.zinc.util.MerkleReplacer
import com.ing.zkflow.gradle.plugin.circuitNames
import com.ing.zkflow.gradle.plugin.platformSourcesRootPath
import com.ing.zkflow.gradle.plugin.zkFlowExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

open class PrepareCircuitForCompilationTask : DefaultTask() {
    private val extension = project.zkFlowExtension

    @InputDirectory
    val zincSources = extension.circuitSourcesBasePath

    @OutputDirectories
    val zincOutputs = project.circuitNames?.map { extension.mergedCircuitOutputPath.resolve(it).resolve("src") }

    @TaskAction
    fun prepareCircuitForCompilation() {
        project.circuitNames?.forEach { circuitName ->
            val circuitSourceOutputPath = extension.mergedCircuitOutputPath.resolve(circuitName).resolve("src")
            val replacer = MerkleReplacer(circuitSourceOutputPath)

            val consts = joinConstFiles(circuitSourceOutputPath, project.platformSourcesRootPath)

            replacer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
            replacer.setCorrespondingMerkleTreeFunctionForMainTree()
        }
    }
}
