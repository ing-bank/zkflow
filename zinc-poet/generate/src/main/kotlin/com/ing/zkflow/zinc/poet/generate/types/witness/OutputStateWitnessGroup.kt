package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.bfl.toZincId
import com.ing.zinc.poet.ZincArray
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincType
import com.ing.zkflow.zinc.poet.generate.COMPUTE_LEAF_HASHES
import com.ing.zkflow.zinc.poet.generate.COMPUTE_NONCE
import com.ing.zkflow.zinc.poet.generate.types.SerializedStateGroup
import com.ing.zkflow.zinc.poet.generate.types.StandardTypes
import com.ing.zkflow.zinc.poet.generate.types.Witness
import net.corda.core.contracts.ComponentGroupEnum

@Suppress("TooManyFunctions")
data class OutputStateWitnessGroup(
    override val groupName: String,
    private val baseName: String,
    private val states: Map<BflModule, Int>,
    val standardTypes: StandardTypes,
) : WitnessGroup {
    private val serializedGroup = SerializedStateGroup(groupName, baseName, standardTypes.toTransactionStates(states))
    internal val deserializedGroup = serializedGroup.deserializedStruct

    private val groupSize: Int = states.entries.sumBy { it.value }
    override val isPresent: Boolean = groupSize > 0
    override val options: List<WitnessGroupOptions> = standardTypes.toWitnessGroupOptions(groupName, states)

    override val dependencies: List<BflType> = listOf(serializedGroup, deserializedGroup)
    override val serializedType: ZincType = serializedGroup.toZincId()
    override val generateHashesMethod = ZincMethod.zincMethod {
        comment = "Compute the $groupName leaf hashes."
        name = "compute_${groupName}_leaf_hashes"
        returnType = ZincArray.zincArray {
            elementType = StandardTypes.nonceDigest.toZincId()
            size = "$groupSize"
        }
        body = """
            let mut nonces = [${StandardTypes.nonceDigest.defaultExpr()}; $groupSize];

            for i in (0 as u32)..$groupSize {
                nonces[i] = $COMPUTE_NONCE(self.${Witness.PRIVACY_SALT}, ${StandardTypes.componentGroupEnum.id}::${ComponentGroupEnum.OUTPUTS_GROUP.name} as u32, i);
            }

            self.$groupName.$COMPUTE_LEAF_HASHES(nonces)
        """.trimIndent()
    }
}
