package com.ing.zknotary.gradle.zinc.template

open class TemplateParameters(
    open val templateFile: String,
    private val dependencies: List<TemplateParameters>
) {
    fun resolveAllConfigurations(): List<TemplateParameters> =
        dependencies.flatMap { it.resolveAllConfigurations() } + this

    open fun getReplacements(): Map<String, String> = emptyMap()
    open fun getTargetFilename(): String = templateFile

    companion object {
        private val camelRegex = "(?<=[a-z])[A-Z]".toRegex()

        internal fun String.camelToSnakeCase(): String {
            return camelRegex.replace(this) {
                "_${it.value}"
            }.toLowerCase()
        }
    }
}
