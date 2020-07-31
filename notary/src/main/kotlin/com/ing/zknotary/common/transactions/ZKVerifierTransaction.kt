package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.MerkleTree
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
class ZKVerifierTransaction(
    val proof: ByteArray,
    val inputs: List<StateRef>,
    val references: List<StateRef>,

    // TODO: we should add some information that the verifier can use to select the correct verifier key?
    // Or do we just attach the hash of the verifier key?
    // With that they can select the correct key, and also know which circuit they are verifiying.
    // Perhaps the command?

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
    val componentNonces: Map<Int, List<SecureHash>>,

    /**
     * Used for padding internal lists to sizes accepted by the ZK circuit.
     */
    componentPaddingConfiguration: ComponentPaddingConfiguration
) {
    val padded = Padded(
        originalInputs = inputs,
        originalOutputs = outputs,
        originalReferences = references,
        originalTimeWindow = timeWindow,
        originalNetworkParametersHash = networkParametersHash,
        paddingConfiguration = componentPaddingConfiguration
    )

    // Required by Corda.
    val componentPaddingConfiguration: ComponentPaddingConfiguration
        get() = padded.paddingConfiguration

    init {
        componentPaddingConfiguration.validate(this)

        require(padded.inputs().size == componentNonces[ComponentGroupEnum.INPUTS_GROUP.ordinal]?.size ?: 0) { "Number of inputs and input nonces should be equal" }
        require(outputHashes.size == componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]?.size ?: 0) { "Number of outputs and output nonces should be equal" }
        require(padded.references().size == componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size ?: 0) { "Number of references (${references.size}) and reference nonces (${componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size}) should be equal" }

        if (networkParametersHash != null) require(componentNonces[ComponentGroupEnum.PARAMETERS_GROUP.ordinal]?.size == 1) { "If there is a networkParametersHash, there should be a networkParametersHash nonce" }
        if (timeWindow != null) require(componentNonces[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal]?.size == 1) { "If there is a timeWindow, there should be exactly one timeWindow nonce" }

        verify()
    }

    val id by lazy { merkleTree.root }

    val merkleTree by lazy {
        ZKPartialMerkleTree(this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)

    data class Padded(
        private val originalInputs: List<StateRef>,
        private val originalOutputs: List<StateRef>,
        private val originalReferences: List<StateRef>,
        private val originalTimeWindow: TimeWindow?,
        private val originalNetworkParametersHash: SecureHash?,
        val paddingConfiguration: ComponentPaddingConfiguration
    ) {

        fun inputs(): List<StateRef> {
            val filler = filler(ComponentGroupEnum.INPUTS_GROUP)
            require(filler is ComponentPaddingConfiguration.Filler.StateRef) { "Expected filler of type ZKStateRef" }
            return originalInputs.pad(sizeOf(ComponentGroupEnum.INPUTS_GROUP), filler.content)
        }
        fun outputs(): List<StateRef> {
            val filler = filler(ComponentGroupEnum.OUTPUTS_GROUP)
            require(filler is ComponentPaddingConfiguration.Filler.StateRef) { "Expected filler of type ZKStateRef" }
            return originalOutputs.pad(sizeOf(ComponentGroupEnum.OUTPUTS_GROUP), filler.content)
        }

        fun references(): List<StateRef> {
            val filler = filler(ComponentGroupEnum.REFERENCES_GROUP)
            require(filler is ComponentPaddingConfiguration.Filler.StateRef) { "Expected filler of type ZKStateRef" }
            return originalReferences.pad(sizeOf(ComponentGroupEnum.REFERENCES_GROUP), filler.content)
        }

        fun timeWindow() = originalTimeWindow.wrappedPad(TimeWindow.fromOnly(Instant.MIN))

        fun networkParametersHash() = originalNetworkParametersHash.wrappedPad(SecureHash.zeroHash)

        /**
         * Return appropriate size or fail.
         */
        private fun sizeOf(componentGroup: ComponentGroupEnum): Int =
            paddingConfiguration.sizeOf(componentGroup) ?: error("Expected a positive number")

        /**
         * Returns appropriate size or fail.
         */
        fun filler(componentGroup: ComponentGroupEnum) =
            paddingConfiguration.filler(componentGroup) ?: error("Expected a filler object")
    }

    fun verify() {
        // Confirm that the known component hashes of the provided outputs component group match with the provided groupHash
        if (outputHashes.isNotEmpty()) {
            val outputsubTree = MerkleTree.getMerkleTree(outputHashes, nodeDigestService, componentGroupLeafDigestService)
            check(outputsubTree.hash == groupHashes[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]) {
                "The provided output hashes do not match the provided outputs group hash"
            }
        }
    }
}
