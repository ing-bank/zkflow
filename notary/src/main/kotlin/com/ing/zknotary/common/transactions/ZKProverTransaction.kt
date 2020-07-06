package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.util.ComponentPaddingConfiguration
import com.ing.zknotary.common.util.PaddingWrapper
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.serialization.CordaSerializable
import java.security.PublicKey

@CordaSerializable
class ZKProverTransaction(
    val inputs: List<ZKStateAndRef<ContractState>>,
    /**
     * Because a ZKStateRef is a representation of the contents of a state, and no longer a pointer to
     * a previous transaction output, outputs are also ZKStateAndRefs, like inputs and references.
     */
    val outputs: List<ZKStateAndRef<ContractState>>,
    val references: List<ZKStateAndRef<ContractState>>,
    val command: Command<*>,
    val notary: Party,
    val timeWindow: TimeWindow?,
    val privacySalt: PrivacySalt,

    /**
     * Decide how to handle the networkparameters. Simplest it to use the hash only
     * since the verifier will also have the hash, but we will need to possibly use the contents for verification logic?
     * The non-validating notary receives the hash as part of the normal ftx and checks the platform version and notary.
     * For now, we will not need the other parameters for verification yet. When we do, we will need to supply both the
     * Parameters and their hash to the verification circuit and ensure that they are the same. Then the hash is part of
     * the instance, so the verifier is convinced that all is linked. Then we can use the parameters in verification logic.
     * In standard Corda, the hash is: SHA256(cordaSerialize(NetworkParameters)), where cordaSerialize is the standard Corda
     * AMQP serialization.
     */
    val networkParametersHash: SecureHash?,

    // For now we ignore attachment contents inside the circuit. We might want to use them for attaching some circuit identifier or even the verifier key.
    val attachments: List<AttachmentId>,

    /**
     * Used for serialization of the merkle tree leaves and for ZKStateRefs
     */
    val serializationFactoryService: SerializationFactoryService,
    val componentGroupLeafDigestService: DigestService,
    val nodeDigestService: DigestService = componentGroupLeafDigestService,

    /**
     * Used for padding internal lists to sizes accepted by the ZK circuit.
     */
    val componentPaddingConfiguration: ComponentPaddingConfiguration
) : NamedByZKMerkleTree {

    val padded = Padded(
        originalInputs = inputs, originalOutputs = outputs, originalSigners = command.signers,
        originalReferences = references, paddingConfiguration = componentPaddingConfiguration
    )

    init {
        componentPaddingConfiguration.validate(this)
    }

    val id by lazy { merkleTree.root }

    /** This additional merkle root is represented by the root hash of a Merkle tree over the transaction components. */
    override val merkleTree by lazy {
        ZKFullMerkleTree(this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun toString() = prettyPrint()
    override fun equals(other: Any?) = if (other !is ZKProverTransaction) false else (this.id == other.id)

    data class Padded(
        private val originalInputs: List<ZKStateAndRef<ContractState>>,
        private val originalOutputs: List<ZKStateAndRef<ContractState>>,
        private val originalSigners: List<PublicKey>,
        private val originalReferences: List<ZKStateAndRef<ContractState>>,
        val paddingConfiguration: ComponentPaddingConfiguration
    ) {

        fun inputs(): List<PaddingWrapper<ZKStateAndRef<ContractState>>> {
            val filler = paddingConfiguration.filler(ComponentGroupEnum.INPUTS_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPaddingConfiguration.Filler.ZKStateAndRef) { "Expected filler of type ZKStateAndRef" }
            return originalInputs.wrappedPad(sizeOf(ComponentGroupEnum.INPUTS_GROUP), filler.content)
        }

        fun outputs(): List<PaddingWrapper<ZKStateAndRef<ContractState>>> {
            val filler = paddingConfiguration.filler(ComponentGroupEnum.OUTPUTS_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPaddingConfiguration.Filler.ZKStateAndRef) { "Expected filler of type ZKStateAndRef" }
            return originalOutputs.wrappedPad(sizeOf(ComponentGroupEnum.OUTPUTS_GROUP), filler.content)
        }

        fun references(): List<PaddingWrapper<ZKStateAndRef<ContractState>>> {
            val filler = paddingConfiguration.filler(ComponentGroupEnum.REFERENCES_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPaddingConfiguration.Filler.ZKStateAndRef) { "Expected filler of type ZKStateAndRef" }
            return originalReferences.wrappedPad(sizeOf(ComponentGroupEnum.REFERENCES_GROUP), filler.content)
        }

        fun signers(): List<PaddingWrapper<PublicKey>> {
            val filler = paddingConfiguration.filler(ComponentGroupEnum.SIGNERS_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPaddingConfiguration.Filler.PublicKey) { "Expected filler of type PublicKey" }
            return originalSigners.wrappedPad(sizeOf(ComponentGroupEnum.SIGNERS_GROUP), filler.content)
        }

        /**
         * Return appropriate size ot fail.
         */
        fun sizeOf(componentGroup: ComponentGroupEnum) =
            paddingConfiguration.sizeOf(componentGroup) ?: error("Expected a positive number")
    }
}
