package com.ing.zinc.bfl.generator

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflWrappedTransactionComponent
import com.ing.zinc.bfl.CONSTS
import com.ing.zinc.bfl.dsl.WrappedTransactionComponentBuilder.Companion.wrappedTransactionComponent
import com.ing.zinc.naming.camelToZincSnakeCase
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincConstant
import com.ing.zinc.poet.ZincConstant.Companion.zincConstant
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.ZincType
import java.util.Locale

/**
 * Configuration options specific to a certain witness group.
 *
 * Contains utilities to aid in generating deserialization methods for this witness group.
 *
 * @property name Name of the witness group. (i.e. inputs, outputs, references, ...)
 * @property type [BflWrappedTransactionComponent] describing the type contained in this witness group
 */
data class TransactionComponentOptions(
    val name: String,
    val type: BflWrappedTransactionComponent
) {
    private val sizeInBits: Int = type.bitSize

    /**
     * [ZincConstant] for the size of this witness group.
     *
     * This utility value can be used to generate "src/consts.zn"
     */
    val witnessSizeConstant = zincConstant {
        name = "WITNESS_SIZE_${this@TransactionComponentOptions.name.toUpperCase(Locale.getDefault())}"
        type = ZincPrimitive.U24
        initialization = "$sizeInBits"
    }

    /**
     * [ZincType] of the witness for this witness group.
     *
     * This utility value can be used when generating functions/methods that take the witness group as parameter
     */
    val witnessType = zincArray {
        elementType = ZincPrimitive.Bool
        size = "$CONSTS::${witnessSizeConstant.getName()}"
    }

    /**
     * Name of the deserialize method for this witness group.
     */
    val deserializeMethodName: String = "deserialize_from_${name.camelToZincSnakeCase()}"

    /**
     * Generate an expression with which to deserialize the unwrapped state from the [witnessGroupVariable].
     */
    fun generateDeserializeExpr(witnessGroupVariable: String): String = type.deserializeLastFieldExpr(
        this,
        offset = "0 as u24",
        witnessGroupVariable = witnessGroupVariable
    )

    companion object {
        fun wrapped(groupName: String, stateClass: BflType): TransactionComponentOptions = TransactionComponentOptions(
            groupName,
            wrappedTransactionComponent {
                name = groupName
                transactionComponent(stateClass)
            }
        )
    }
}
