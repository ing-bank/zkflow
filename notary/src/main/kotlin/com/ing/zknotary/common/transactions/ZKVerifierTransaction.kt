package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.states.ZKStateRef
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
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

    // TODO: we should add some information that the verifier can use to select the correct verifier key?
    // Or do we just attach the hash of the verifier key?
    // With that they can select the correct key, and also know which circuit they are verifiying.

    val notary: Party,
    val timeWindow: TimeWindow?,
    val networkParametersHash: SecureHash?,

    val componentGroupLeafDigestService: DigestService,
    val nodeDigestService: DigestService = componentGroupLeafDigestService,

    val groupHashes: List<SecureHash>,
    val componentNonces: Map<Int, List<SecureHash>>,

    /**
     * Used for padding internal lists to sizes accepted by the ZK circuit.
     */
    componentPaddingConfiguration: ComponentPaddingConfiguration
) {
    val padded = Padded(
        originalInputs = inputs, originalOutputs = outputs,
        originalReferences = references, paddingConfiguration = componentPaddingConfiguration
    )

    // Required by Corda.
    val componentPaddingConfiguration: ComponentPaddingConfiguration
        get() = padded.paddingConfiguration

    init {
        componentPaddingConfiguration.validate(this)

        require(padded.inputs().size == componentNonces[ComponentGroupEnum.INPUTS_GROUP.ordinal]?.size ?: 0) { "Number of inputs and input nonces should be equal" }
        require(padded.outputs().size == componentNonces[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]?.size ?: 0) { "Number of outputs and output nonces should be equal" }
        require(padded.references().size == componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size ?: 0) { "Number of references (${references.size}) and reference nonces (${componentNonces[ComponentGroupEnum.REFERENCES_GROUP.ordinal]?.size}) should be equal" }

        if (networkParametersHash != null) require(componentNonces[ComponentGroupEnum.PARAMETERS_GROUP.ordinal]?.size == 1) { "If there is a networkParametersHash, there should be a networkParametersHash nonce" }
        if (timeWindow != null) require(componentNonces[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal]?.size == 1) { "If there is a timeWindow, there should be exactly one timeWindow nonce" }
    }

    val id by lazy { merkleTree.root }

    val merkleTree by lazy {
        ZKPartialMerkleTree(this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?) = if (other !is ZKVerifierTransaction) false else (this.id == other.id)

    data class Padded(
        private val originalInputs: List<ZKStateRef>,
        private val originalOutputs: List<ZKStateRef>,
        private val originalReferences: List<ZKStateRef>,
        val paddingConfiguration: ComponentPaddingConfiguration
    ) {

        fun inputs(): List<ZKStateRef> {
            val filler = paddingConfiguration.filler(ComponentGroupEnum.INPUTS_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPaddingConfiguration.Filler.ZKStateRef) { "Expected filler of type ZKStateRef" }
            return originalInputs.pad(sizeOf(ComponentGroupEnum.INPUTS_GROUP), filler.content)
        }
        fun outputs(): List<ZKStateRef> {
            val filler = paddingConfiguration.filler(ComponentGroupEnum.OUTPUTS_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPaddingConfiguration.Filler.ZKStateRef) { "Expected filler of type ZKStateRef" }
            return originalOutputs.pad(sizeOf(ComponentGroupEnum.OUTPUTS_GROUP), filler.content)
        }

        fun references(): List<ZKStateRef> {
            val filler = paddingConfiguration.filler(ComponentGroupEnum.REFERENCES_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPaddingConfiguration.Filler.ZKStateRef) { "Expected filler of type ZKStateRef" }
            return originalReferences.pad(sizeOf(ComponentGroupEnum.REFERENCES_GROUP), filler.content)
        }

        /**
         * Return appropriate size ot fail.
         */
        private fun sizeOf(componentGroup: ComponentGroupEnum): Int =
            paddingConfiguration.sizeOf(componentGroup) ?: error("Expected a positive number")
    }
}
