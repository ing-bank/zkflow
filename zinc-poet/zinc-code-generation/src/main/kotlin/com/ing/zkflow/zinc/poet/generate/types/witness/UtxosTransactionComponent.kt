package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.bfl.getSerializedTypeDef
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.ZincArray.Companion.zincArray
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.zinc.poet.generate.COMPUTE_UTXO_HASHES
import com.ing.zkflow.zinc.poet.generate.types.IndexedTransactionComponent
import com.ing.zkflow.zinc.poet.generate.types.SerializedStateGroup
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes

internal data class UtxosTransactionComponent(
    override val groupName: String,
    private val baseName: String,
    private val states: List<IndexedTransactionComponent>,
    private val noncesVariable: String,
    private val standardTypes: StandardTypes,
) : TransactionComponent {
    private val serializedGroup = SerializedStateGroup(groupName, baseName, states)
    internal val deserializedGroup = serializedGroup.deserializedStruct

    private val groupSize: Int = states.size
    override val isPresent: Boolean = groupSize > 0
    override val options: List<TransactionComponentOptions> = standardTypes.toTransactionComponentOptions(states)
    override val dependencies: List<BflType> = options.map { it.type } + listOf(serializedGroup, deserializedGroup)
    override val serializedType: ZincType = serializedGroup.toZincId()
    override val generateHashesMethod = zincMethod {
        comment = "Compute the $groupName leaf hashes."
        name = "compute_${groupName}_hashes"
        returnType = zincArray {
            elementType = StandardTypes.digest.getSerializedTypeDef()
            size = "$groupSize"
        }
        body = "self.$groupName.$COMPUTE_UTXO_HASHES(self.$noncesVariable)"
    }
}
