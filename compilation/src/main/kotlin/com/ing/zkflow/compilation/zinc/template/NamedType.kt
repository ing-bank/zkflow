package com.ing.zkflow.compilation.zinc.template

import com.ing.zkflow.util.camelToSnakeCase

/**
 * A [NamedType] reflects a template that contains a named type.
 *
 * This can be used both when generating the template itself and when generating templates that depends on this type.
 *
 * @property typeName the name of the type
 */
public interface NamedType {
    public val typeName: String

    /**
     * Returns the module name, for use in `mod` and `use` statements.
     */
    public fun getModuleName(): String = typeName.camelToSnakeCase()

    /**
     * Returns the constant prefix, to apply on constants in the template.
     */
    public fun getConstantPrefix(): String = getModuleName().toUpperCase()

    /**
     * Returns the filename for this type.
     */
    public fun getFileName(): String = "${getModuleName()}.zn"

    /**
     * Returns the common replacements for a type.
     *
     * These are the name, the constant prefix and the module name.
     */
    fun getTypeReplacements(prefix: String = "") = mapOf(
        "${prefix}TYPE_NAME" to typeName,
        "${prefix}CONSTANT_PREFIX" to getConstantPrefix(),
        "${prefix}MODULE_NAME" to getModuleName(),
    )
}
