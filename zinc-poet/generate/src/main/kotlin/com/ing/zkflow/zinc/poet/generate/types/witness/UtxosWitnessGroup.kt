package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.zinc.poet.generate.COMPUTE_UTXO_HASHES
import com.ing.zkflow.zinc.poet.generate.types.SerializedStateGroup
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes

data class UtxosWitnessGroup(
    override val groupName: String,
    private val baseName: String,
    private val states: Map<BflModule, Int>,
    private val noncesVariable: String,
    private val standardTypes: StandardTypes,
    private val commandMetadata: ResolvedZKCommandMetadata,
) : WitnessGroup {
    private val serializedGroup = SerializedStateGroup(groupName, baseName, standardTypes.toTransactionStates(states, commandMetadata))
    internal val deserializedGroup = serializedGroup.deserializedStruct

    private val groupSize: Int = states.entries.sumBy { it.value }
    override val isPresent: Boolean = groupSize > 0
    override val options: List<WitnessGroupOptions> = standardTypes.toWitnessGroupOptions(groupName, states, commandMetadata)
    override val dependencies: List<BflType> = listOf(serializedGroup, deserializedGroup)
    override val serializedType: ZincType = serializedGroup.toZincId()
    override val generateHashesMethod = ZincMethod.zincMethod {
        comment = "Compute the $groupName leaf hashes."
        name = "compute_${groupName}_hashes"
        returnType = ZincArray.zincArray {
            elementType = StandardTypes.digest.toZincId()
            size = "$groupSize"
        }
        body = """
            self.$groupName.$COMPUTE_UTXO_HASHES(self.$noncesVariable)
        """.trimIndent()
    }
}
