package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.naming.camelToSnakeCase

data class IndexedState(
    val index: Int,
    val state: BflModule,
) {
    val fieldName by lazy {
        state.typeName()
            .removeSuffix("TransactionState")
            .camelToSnakeCase() + "_$index"
    }
}
