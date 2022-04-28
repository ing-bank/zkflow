package com.ing.zkflow.zinc.poet.generate.types.witness

import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.poet.ZincMethod
import com.ing.zinc.poet.ZincType

/**
 * This [TransactionComponent] is used to capture witness fields containing additional data used in the hashing functions.
 * There will be no hashes calculated for this group.
 *
 * Witness fields:
 * - privacy_salt
 * - input_nonces
 * - reference_nonces
 */
internal data class HashingMetadataTransactionComponent(
    override val groupName: String,
    val module: BflType,
    override val serializedType: ZincType,
    private val groupSize: Int,
) : TransactionComponent {
    override val isPresent: Boolean = groupSize > 0
    override val options: List<TransactionComponentOptions> = emptyList()
    override val dependencies: List<BflType> = listOf(module)
    override val generateHashesMethod: ZincMethod? = null
}
