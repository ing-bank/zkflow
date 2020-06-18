package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateRef
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class ZKVerifierTransaction(
    val inputs: List<ZKStateRef>,
    val outputs: List<ZKStateRef>,
    val references: List<ZKStateRef>,

    val notary: Party,
    val timeWindow: TimeWindow?,
    val networkParametersHash: SecureHash?,

    val serializationFactoryService: SerializationFactoryService,
    val componentGroupLeafDigestService: DigestService,
    val nodeDigestService: DigestService = componentGroupLeafDigestService,

    val groupHashes: List<SecureHash>,
    val componentNonces: Map<Int, List<SecureHash>>
) {
    init {
        require(inputs.size == componentNonces[ComponentGroupEnum.INPUTS_GROUP.ordinal]?.size ?: 0) { "Number of inputs and input nonces should be equal" }
        require(outputs.size == componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]?.size ?: 0) { "Number of outputs and output nonces should be equal" }
        require(references.size == componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size ?: 0) { "Number of references (${references.size}) and reference nonces (${componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size}) should be equal" }

        if (networkParametersHash != null) require(componentNonces[ComponentGroupEnum.PARAMETERS_GROUP.ordinal]?.size == 1) { "If there is a networkParametersHash, there should be a networkParametersHash nonce" }
        if (timeWindow != null) require(componentNonces[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal]?.size == 1) { "If there is a timeWindow, there should be exactly one timeWindow nonce" }
    }

    val id by lazy { merkleTree.root }

    val merkleTree by lazy {
        ZKPartialMerkleTree(
            this,
            serializationFactoryService,
            componentGroupLeafDigestService,
            nodeDigestService
        )
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)
}
