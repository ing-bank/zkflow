package com.ing.zknotary.gradle.util

import java.io.File

class Renderer(private val outputPath: File) {

    fun operateTemplateRenderer(platformTemplates: Array<File>, consts: String, bigDecimalSizes: Set<Pair<Int, Int>>) {
        val renderer = TemplateRenderer(outputPath)

        renderer.generateFloatingPointsCode(
            getTemplateContents(platformTemplates, "floating_point.zn"),
            bigDecimalSizes
        )

        renderer.generateMerkleUtilsCode(
            getTemplateContents(platformTemplates, "merkle_template.zn"),
            consts
        )

        renderer.generateMainCode(
            getTemplateContents(platformTemplates, "main_template.zn"),
            consts
        )
    }

    fun operateMerkleRenderer(consts: String) {
        val renderer = MerkleRenderer(outputPath)

        renderer.setCorrespondingMerkleTreeFunctionForComponentGroups(consts)
        renderer.setCorrespondingMerkleTreeFunctionForMainTree(consts)
    }

    fun operateCopyRenderer(platformSources: Array<File>, circuitSourcesBase: File, circuitName: String, projectVersion: String) {
        val renderer = CopyRenderer(outputPath)

        renderer.createCopyZincCircuitSources(circuitSourcesBase, circuitName, projectVersion)
        renderer.createCopyZincPlatformSources(platformSources)
    }

    private fun getTemplateContents(platformTemplates: Array<File>, templateFileName: String): String {
        return platformTemplates.single { it.name.contains(templateFileName) }.readText()
    }
}
