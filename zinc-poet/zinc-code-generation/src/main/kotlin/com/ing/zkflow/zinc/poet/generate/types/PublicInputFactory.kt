package com.ing.zkflow.zinc.poet.generate.types

import com.ing.zinc.bfl.BflStruct
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.bfl.getSerializedBflTypeDef
import com.ing.zinc.poet.ZincArray
import com.ing.zkflow.zinc.poet.generate.types.witness.TransactionComponentContainer

const val PUBLIC_INPUT = "PublicInput"

class PublicInputFactory(
    private val transactionComponentContainer: TransactionComponentContainer
) {
    fun create(): BflStruct {
        return struct {
            name = PUBLIC_INPUT
            (transactionComponentContainer.transactionComponents).forEach { transactionComponent ->
                transactionComponent.generateHashesMethod?.let {
                    val groupSize = (it.getReturnType() as ZincArray).getSize()
                    field {
                        name = transactionComponent.publicInputFieldName
                        type = array {
                            capacity = groupSize.toInt()
                            elementType = StandardTypes.digest.getSerializedBflTypeDef()
                        }
                    }
                }
            }
            isDeserializable = false
        }
    }
}
