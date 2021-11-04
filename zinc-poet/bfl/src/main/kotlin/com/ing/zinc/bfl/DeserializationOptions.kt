package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.WitnessGroupOptions

/**
 * @property witnessGroupOptions
 * @property bitArrayVariable the variable holding the byte array
 * @property offset the offset in the byte array
 * @property variablePrefix prefix to use for local variables, to avoid name clashes
 */
data class DeserializationOptions(
    val witnessGroupOptions: WitnessGroupOptions,
    val bitArrayVariable: String,
    val offset: String,
    val variablePrefix: String,
) {
    fun generateVariable(name: String) = "${variablePrefix}_$name"

    fun deserializeModule(module: BflModule) =
        "${module.id}::${witnessGroupOptions.deserializeMethodName}($bitArrayVariable, $offset)"
}
