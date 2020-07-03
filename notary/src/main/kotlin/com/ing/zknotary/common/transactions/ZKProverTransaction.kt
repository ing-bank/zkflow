package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.util.ComponentPadding
import com.ing.zknotary.common.util.Nature
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
    val componentPadding: ComponentPadding
) : NamedByZKMerkleTree {

    val padded = Padded(
        originalInputs = inputs, originalOutputs = outputs, originalSigners = command.signers,
        originalReferences = references, padding = componentPadding
    )

    init {
        componentPadding.validate(this)
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
        val padding: ComponentPadding
    ) {

        fun inputs(): List<Nature<ZKStateAndRef<ContractState>>> {
            val filler = padding.filler(ComponentGroupEnum.INPUTS_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPadding.Filler.ZKStateAndRef) { "Expected filler of type ZKStateAndRef" }
            return originalInputs
                .map { Nature.Authentic(it) }
                .pad(sizeOf(ComponentGroupEnum.INPUTS_GROUP), Nature.Bogus(filler.value))
        }

        fun outputs(): List<Nature<ZKStateAndRef<ContractState>>> {
            val filler = padding.filler(ComponentGroupEnum.OUTPUTS_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPadding.Filler.ZKStateAndRef) { "Expected filler of type ZKStateAndRef" }
            return originalOutputs
                .map { Nature.Authentic(it) }
                .pad(sizeOf(ComponentGroupEnum.OUTPUTS_GROUP), Nature.Bogus(filler.value))
        }

        fun references(): List<Nature<ZKStateAndRef<ContractState>>> {
            val filler = padding.filler(ComponentGroupEnum.REFERENCES_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPadding.Filler.ZKStateAndRef) { "Expected filler of type ZKStateAndRef" }
            return originalReferences
                .map { Nature.Authentic(it) }
                .pad(sizeOf(ComponentGroupEnum.REFERENCES_GROUP), Nature.Bogus(filler.value))
        }

        fun signers(): List<Nature<PublicKey>> {
            val filler = padding.filler(ComponentGroupEnum.SIGNERS_GROUP) ?: error("Expected a filler object")
            require(filler is ComponentPadding.Filler.PublicKey) { "Expected filler of type PublicKey" }
            return originalSigners
                .map { Nature.Authentic(it) }
                .pad(sizeOf(ComponentGroupEnum.SIGNERS_GROUP), Nature.Bogus(filler.value))
        }

        /**
         * Return appropriate size ot fail.
         */
        fun sizeOf(componentGroup: ComponentGroupEnum) =
            padding.sizeOf(componentGroup) ?: error("Expected a positive number")
    }
}
