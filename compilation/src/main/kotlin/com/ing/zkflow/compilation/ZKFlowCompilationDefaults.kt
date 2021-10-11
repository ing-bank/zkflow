package com.ing.zkflow.compilation

public object ZKFlowCompilationDefaults {
    /**
     *  FIXME: obsolete, but kept here until we rebuild Zinc code generation to use the ZKTransactionMetadata
     *  instead of this (with the configurator class,  which can then also be deleted).
     */
    public const val DEFAULT_CONFIG_CIRCUIT_FILE: String = "config.json"
}
