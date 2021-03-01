package com.ing.zknotary.common.transactions

import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@Suppress("LongParameterList")
@CordaSerializable
class ZKVerifierTransaction(
    val proof: ByteArray,
    val inputs: List<StateRef>,
    val references: List<StateRef>,

    // TODO: we should add some information that the verifier can use to select the correct verifier key?
    // Or do we just attach the hash of the verifier key?
    // With that they can select the correct key, and also know which circuit they are verifying.
    // Perhaps the command?
    val circuitId: SecureHash,

    // If we include command instead of circuitId then we can take signers from it
    val signers: List<PublicKey>,

    val notary: Party,
    val timeWindow: TimeWindow?,
    val networkParametersHash: SecureHash?,

    val componentGroupLeafDigestService: DigestService,
    val nodeDigestService: DigestService = componentGroupLeafDigestService,

    // Outputs are not visible in a normal FilteredTransaction, so we 'leak' some info here: the amount of outputs.
    // Outputs are the leaf hashes of the outputs component group. This is the only group where:
    // * we don't provide the leaf contents but provide the leaf hashes. In other cases we provide either all contents
    //   of all leaves, or we provide nothing (hide all leaves completely) and we just use the component group hash
    //   to build the Merkle tree. In the case of outputs, verifiers need to
    //   be able to see the component leaf hashes of past transactions in the backchain, so that they can:
    //   * For each input StateRef in the head transaction, find the matching output hash in the previous tx. Then for the proof verification
    //     they provide this list of output hashes (for the inputs being consumed) as public input. The circuit will enforce
    //     that for each input contents from the witness,  when combined with their nonce, should hash to the same hash as
    //     provided for that input in the public input.
    val outputHashes: List<SecureHash>,
    val groupHashes: List<SecureHash>,
    val componentNonces: Map<Int, List<SecureHash>>
) : NamedByZKMerkleTree {

    init {
        // Nonces for the outputs should NEVER be present
        require(!componentNonces.containsKey(ComponentGroupEnum.OUTPUTS_GROUP.ordinal))

        require(groupHashes.size == ComponentGroupEnum.values().size) { "There should be a group hash for each ComponentGroupEnum value" }
        require(inputs.size == componentNonces[ComponentGroupEnum.INPUTS_GROUP.ordinal]?.size ?: 0) { "Number of inputs and input nonces should be equal" }
        require(references.size == componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size ?: 0) { "Number of references (${references.size}) and reference nonces (${componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size}) should be equal" }

        require(signers.size == componentNonces[ComponentGroupEnum.SIGNERS_GROUP.ordinal]?.size ?: 0) { "Number of signers and signer nonces should be equal" }

        if (networkParametersHash != null) require(componentNonces[ComponentGroupEnum.PARAMETERS_GROUP.ordinal]?.size == 1) { "If there is a networkParametersHash, there should be a networkParametersHash nonce" }
        if (timeWindow != null) require(componentNonces[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal]?.size == 1) { "If there is a timeWindow, there should be exactly one timeWindow nonce" }
    }

    override val id by lazy { merkleTree.root }

    override val merkleTree by lazy {
        ZKPartialMerkleTree(this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)
}
