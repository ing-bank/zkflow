package com.ing.zknotary.gradle.zinc.template

import com.ing.zknotary.gradle.zinc.util.createOutputFile
import java.nio.file.Path

class TemplateRenderer(
    private val outputDirectory: Path,
    private val templateLoader: TemplateLoader
) {
    fun renderTemplate(templateParameters: TemplateParameters) {
        val templateContents =
            renderTemplateWarning(templateParameters) + templateLoader.loadTemplate(templateParameters)
        val content = templateParameters.getReplacements().entries
            .fold(templateContents) { content, replacement ->
                content.replace("\${${replacement.key}}", replacement.value)
            }
        createOutputFile(outputDirectory.resolve(templateParameters.getTargetFilename()))
            .writeBytes(content.toByteArray())
    }

    private fun renderTemplateWarning(
        templateParameters: TemplateParameters
    ): String {
        return """
            //! GENERATED CODE. DO NOT EDIT
            //! Edit the template in zinc-platform-sources/src/main/resources/zinc-platform-templates/${templateParameters.templateFile}
            //
        """.trimIndent() + "\n"
    }
}
