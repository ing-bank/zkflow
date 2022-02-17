package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.WitnessGroupOptions
import com.ing.zinc.poet.ZincFunction
import com.ing.zinc.poet.ZincType

/**
 * This [WitnessGroup] is used to capture witness fields containing additional data used in the hashing functions.
 * There will be no hashes calculated for this group.
 *
 * Witness fields:
 * - privacy_salt
 * - input_nonces
 * - reference_nonces
 */
internal data class HashingMetadataWitnessGroup(
    override val groupName: String,
    val module: BflType,
    override val serializedType: ZincType,
    private val groupSize: Int,
) : WitnessGroup {
    override val isPresent: Boolean = groupSize > 0
    override val options: List<WitnessGroupOptions> = emptyList()
    override val dependencies: List<BflType> = listOf(module)
    override val generateHashesMethod: ZincFunction? = null
}
