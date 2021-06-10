package com.ing.zknotary.gradle.zinc.template

/**
 * The parameters needed to render [templateFile].
 *
 * Templates are located in `zinc-platform-sources/src/main/resources/zinc-platform-templates`. Templates are rendered
 * by performing string replacements for `${VARIABLE_PLACEHOLDER}` occurrences in the template file. The
 * [TemplateParameters] hold all the replacements that are needed for this specific template.
 *
 * For each template file in `zinc-platform-templates` there must be a corresponding [TemplateParameters] that provides
 * the relevant replacements and the target file name.
 *
 * Dependencies between templates are captured in the [dependencies] field. This reduces configuration overhead, where
 * all dependencies have to be defined explicitly, without easily knowing why certain templates are needed.
 *
 * @property templateFile the filename of the template under
 * @property dependencies list of [TemplateParameters] for types this instance depends on
 */
open class TemplateParameters(
    val templateFile: String,
    private val dependencies: List<TemplateParameters>
) : NamedType {
    override val typeName = templateFile.removeSuffix(".zn").snakeCaseToCamel()

    /**
     * Recursively resolves all configurations required in this template.
     */
    internal fun resolveAllConfigurations(): List<TemplateParameters> =
        dependencies.flatMap { it.resolveAllConfigurations() } + this

    /**
     * The replacements to apply for this template.
     *
     * This should return a map where the key is the variable name, e.g. VARIABLE_PLACEHOLDER, and the value is the
     * replacement. For example:
     * ```
     * mapOf(
     *     "TYPE_NAME" to "MyCustomType",
     *     "VARIABLE_SIZE" to "42",
     * )
     * ```
     */
    open fun getReplacements(): Map<String, String> = emptyMap()

    /**
     * The filename of the file to generate, without directory.
     */
    open fun getTargetFilename(): String = templateFile

    companion object {
        private val camelRegex = "(?<=[a-z])[A-Z]".toRegex()

        internal fun String.camelToSnakeCase(): String {
            return camelRegex.replace(this) {
                "_${it.value}"
            }.toLowerCase()
        }

        internal fun String.snakeCaseToCamel(): String = this
            .split("_")
            .joinToString(separator = "") { word -> word.firstCharToUpperCase() }

        internal fun String.firstCharToUpperCase(): String =
            if (isNotEmpty()) { this[0].toUpperCase() + substring(1) } else this
    }
}
