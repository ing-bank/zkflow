package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.dactyloscopy.Dactyloscopist
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.ComponentGroup
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import java.security.PublicKey
import java.util.function.Predicate

@Suppress("LongParameterList")
@CordaSerializable
class ZKVerifierTransaction(
    wtx: WireTransaction,
    val proof: ByteArray
) : TraversableTransaction(createComponentGroups(wtx), wtx.digestService) {

    // Since we don't store Command itself we need to get signers from it. Should be removed when/if we use Command.
    val signers: List<PublicKey> = commands.single().signers

    // TODO: we should add some information that the verifier can use to select the correct verifier key?
    // Or do we just attach the hash of the verifier key?
    // With that they can select the correct key, and also know which circuit they are verifying.
    // Perhaps the command?
    val circuitId: SecureHash = wtx.zkCommandData().circuitId()

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
    val outputHashes: List<SecureHash>

    lateinit var groupHashes: List<SecureHash>

    override val id: SecureHash by lazy { MerkleTree.getMerkleTree(groupHashes, digestService).hash }

    init {

        // We turn wtx into ftx to get access to nonces and hashes, they are internal in wtx but visible in ftx
        val ftx = wtx.buildFilteredTransaction(Predicate { true })

        // IMPORTANT: this should only include the nonces for the components that are visible in the ZKVerifierTransaction
        val componentNonces = ftx.filteredComponentGroups.filter {
            it.groupIndex in listOf(
                ComponentGroupEnum.INPUTS_GROUP.ordinal,
                ComponentGroupEnum.REFERENCES_GROUP.ordinal,
                ComponentGroupEnum.NOTARY_GROUP.ordinal,
                ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal,
                ComponentGroupEnum.PARAMETERS_GROUP.ordinal,
                ComponentGroupEnum.SIGNERS_GROUP.ordinal
            )
        }.map { it.groupIndex to it.nonces }.toMap()

        outputHashes = availableComponentHashes(ComponentGroupEnum.OUTPUTS_GROUP.ordinal, wtx, ftx)
        groupHashes = ftx.groupHashes

        // Nonces for the outputs should NEVER be present
        require(!componentNonces.containsKey(ComponentGroupEnum.OUTPUTS_GROUP.ordinal))

        require(groupHashes.size == ComponentGroupEnum.values().size) { "There should be a group hash for each ComponentGroupEnum value" }
        require(inputs.size == componentNonces[ComponentGroupEnum.INPUTS_GROUP.ordinal]?.size ?: 0) { "Number of inputs and input nonces should be equal" }
        require(references.size == componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size ?: 0) { "Number of references (${references.size}) and reference nonces (${componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size}) should be equal" }

        require(signers.size == componentNonces[ComponentGroupEnum.SIGNERS_GROUP.ordinal]?.size ?: 0) { "Number of signers and signer nonces should be equal" }

        if (networkParametersHash != null) require(componentNonces[ComponentGroupEnum.PARAMETERS_GROUP.ordinal]?.size == 1) { "If there is a networkParametersHash, there should be a networkParametersHash nonce" }
        if (timeWindow != null) require(componentNonces[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal]?.size == 1) { "If there is a timeWindow, there should be exactly one timeWindow nonce" }
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)

    companion object {
        private fun createComponentGroups(wtx: WireTransaction): List<ComponentGroup> =
            mutableListOf<ComponentGroup>().apply {
                addGroups(
                    mapOf(
                        ComponentGroupEnum.INPUTS_GROUP to
                            wtx.inputs.map { Dactyloscopist.identify(it) },
                        ComponentGroupEnum.REFERENCES_GROUP to
                            wtx.references.map { Dactyloscopist.identify(it) },
                        ComponentGroupEnum.NOTARY_GROUP to
                            listOf(Dactyloscopist.identify(wtx.notary ?: error("Notary should be set"))),
                        ComponentGroupEnum.TIMEWINDOW_GROUP to
                            if (wtx.timeWindow != null) listOf(Dactyloscopist.identify(wtx.timeWindow!!)) else emptyList(), // TODO stub instead of empty list?
                        ComponentGroupEnum.SIGNERS_GROUP to
                            wtx.commands.single().signers.map { Dactyloscopist.identify(it) },
                        ComponentGroupEnum.PARAMETERS_GROUP to
                            listOf(Dactyloscopist.identify(wtx.networkParametersHash ?: error("Network parameters should be set"))),
                        ComponentGroupEnum.COMMANDS_GROUP to
                            wtx.commands.map { Dactyloscopist.identify(it) }
                    )
                )
            }
    }
}
