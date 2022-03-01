package com.ing.zinc.bfl.generator

/**
 * Configuration options to customize the generated code.
 *
 * @property witnessGroupOptions Configurations for the different witness groups.
 */
data class CodeGenerationOptions(
    val witnessGroupOptions: List<WitnessGroupOptions>
)
