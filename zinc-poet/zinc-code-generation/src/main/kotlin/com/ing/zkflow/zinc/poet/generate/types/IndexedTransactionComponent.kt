package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.bfl.BflWrappedTransactionComponent
import com.ing.zinc.bfl.dsl.FieldBuilder.Companion.field
import com.ing.zinc.naming.camelToSnakeCase

data class IndexedTransactionComponent(
    val index: Int,
    val transactionComponent: BflWrappedTransactionComponent,
) {
    val fieldName by lazy {
        transactionComponent.lastField.type.typeName()
            .removeSuffix("TransactionState")
            .camelToSnakeCase() + "_$index"
    }

    fun toDeserializedField(): BflStructField {
        return field {
            name = fieldName
            type = transactionComponent.lastField.type
        }
    }
}
