package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.bfl.BflWrappedState
import com.ing.zinc.bfl.dsl.FieldBuilder.Companion.field
import com.ing.zinc.naming.camelToSnakeCase

data class IndexedState(
    val index: Int,
    val state: BflWrappedState,
) {
    val fieldName by lazy {
        state.lastField.type.typeName()
            .removeSuffix("TransactionState")
            .camelToSnakeCase() + "_$index"
    }

    fun toDeserializedField(): BflStructField {
        return field {
            name = fieldName
            type = state.lastField.type
        }
    }
}
