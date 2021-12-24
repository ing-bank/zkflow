package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflStructField
import com.ing.zinc.bfl.dsl.FieldBuilder.Companion.field
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.naming.camelToSnakeCase

internal fun Map<BflModule, Int>.toFieldList(): List<BflStructField> = map { (stateType, count) ->
    field {
        name = stateTypeFieldName(stateType)
        type = list {
            capacity = count
            elementType = stateType
        }
    }
}

internal fun stateTypeFieldName(stateType: BflModule) = stateType.typeName()
    .removeSuffix("TransactionState")
    .camelToSnakeCase()
