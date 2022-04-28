package com.ing.zinc.bfl.generator

/**
 * Configuration options to customize the generated code.
 *
 * @property transactionComponentOptions Configurations for the different transaction components.
 */
data class CodeGenerationOptions(
    val transactionComponentOptions: List<TransactionComponentOptions>
)
