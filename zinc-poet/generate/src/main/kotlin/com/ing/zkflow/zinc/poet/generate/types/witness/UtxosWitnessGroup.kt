package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.zinc.poet.generate.COMPUTE_UTXO_HASHES
import com.ing.zkflow.zinc.poet.generate.types.SerializedStateGroup
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.StateAndRefsGroupFactory

data class UtxosWitnessGroup(
    override val groupName: String,
    private val baseName: String,
    private val states: Map<BflModule, Int>,
    private val noncesVariable: String,
    private val standardTypes: StandardTypes,
    private val stateAndRefsGroupFactory: StateAndRefsGroupFactory,
) : WitnessGroup {
    private val serializedGroup = SerializedStateGroup(groupName, baseName, standardTypes.toTransactionStates(states))
    private val deserializedGroup = serializedGroup.deserializedStruct

    val ledgerGroup = stateAndRefsGroupFactory.createStructWithStateAndRefs(
        baseName.replace("Utxos", "Group"),
        states,
        deserializedGroup
    )

    private val groupSize: Int = states.entries.sumBy { it.value }
    override val isPresent: Boolean = groupSize > 0
    override val options: List<WitnessGroupOptions> = standardTypes.toWitnessGroupOptions(groupName, states)
    override val dependencies: List<BflType> = listOf(serializedGroup, deserializedGroup, ledgerGroup)
    override val serializedType: ZincType = serializedGroup.toZincId()
    override val generateHashesMethod = generateComputeHashesMethodForUtxos()

    private fun generateComputeHashesMethodForUtxos(): ZincFunction = ZincMethod.zincMethod {
        comment = "Compute the $groupName leaf hashes."
        name = "compute_${groupName}_hashes"
        returnType = ZincArray.zincArray {
            elementType = StandardTypes.nonceDigest.toZincId()
            size = "$groupSize"
        }
        body = """
            self.$groupName.$COMPUTE_UTXO_HASHES(self.$noncesVariable)
        """.trimIndent()
    }
}
