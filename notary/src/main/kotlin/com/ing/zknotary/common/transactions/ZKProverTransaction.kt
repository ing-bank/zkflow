package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.contracts.EnumerableCommand
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.states.ZKStateRef
import com.ing.zknotary.common.zkp.ZKNulls
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.requireThat
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
    private val componentPadding: Map<ComponentGroupEnum, Int>
) : NamedByZKMerkleTree {

    init {
        requireThat {
            "Size of Inputs group must be defined with a positive number" using (
                componentPadding.getOrDefault(ComponentGroupEnum.INPUTS_GROUP, -1) > 0)
            "Size of Outputs group must be defined with a positive number" using (
                componentPadding.getOrDefault(ComponentGroupEnum.OUTPUTS_GROUP, -1) > 0)
            "Size of References group must be defined with a positive number" using (
                componentPadding.getOrDefault(ComponentGroupEnum.REFERENCES_GROUP, -1) > 0)

            "Inputs' size exceeds quantity accepted by ZK circuit" using (
                inputs.size <= componentPadding.getOrDefault(
                    ComponentGroupEnum.INPUTS_GROUP, inputs.size - 1
                ))
            "Outputs' size exceeds quantity accepted by ZK circuit" using (
                outputs.size <= componentPadding.getOrDefault(
                    ComponentGroupEnum.OUTPUTS_GROUP, outputs.size - 1
                ))
            "References' size exceeds quantity accepted by ZK circuit" using (
                references.size <= componentPadding.getOrDefault(
                    ComponentGroupEnum.REFERENCES_GROUP, references.size - 1
                ))

            "Command must have a numeric representation" using (command.value is EnumerableCommand)
        }
    }

    val id by lazy { merkleTree.root }

    val padded = Padded(
        _inputs = inputs, _outputs = outputs, _signers = command.signers,
        _references = references, _padding = componentPadding)

    /** This additional merkle root is represented by the root hash of a Merkle tree over the transaction components. */
    override val merkleTree by lazy {
        ZKFullMerkleTree(this)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun toString() = prettyPrint()
    override fun equals(other: Any?) = if (other !is ZKProverTransaction) false else (this.id == other.id)

    data class Padded(
        private val _inputs: List<ZKStateAndRef<ContractState>>,
        private val _outputs: List<ZKStateAndRef<ContractState>>,
        private val _signers: List<PublicKey>,
        private val _references: List<ZKStateAndRef<ContractState>>,
        private val _padding: Map<ComponentGroupEnum, Int>) {

        val inputs by lazy {
            _inputs.pad(
                _padding[ComponentGroupEnum.INPUTS_GROUP] ?: error("Expected a positive number"),
                fillerZKStateAndRef)
        }
        val outputs by lazy {
            _outputs.pad(
                _padding[ComponentGroupEnum.OUTPUTS_GROUP] ?: error("Expected a positive number"),
                fillerZKStateAndRef)
        }

        val references by lazy {
            _references.pad(
                _padding[ComponentGroupEnum.REFERENCES_GROUP] ?: error("Expected a positive number"),
                fillerZKStateAndRef)
        }

        val signers by lazy {
            _signers.pad(
                _padding[ComponentGroupEnum.SIGNERS_GROUP] ?: error("Expected a positive number"),
                ZKNulls.NULL_PUBLIC_KEY)
        }

        companion object {
            private val fillerZKStateAndRef by lazy {
                // State is hard-coded.
                // There are two possibilities to address this.
                // - Have an interface that requires instances to be able to produce own empty version.
                //   To make use of it, we need to have at least one instance implementing this interface.
                //   In this particular case it leads to requirement that one of the lists
                //   `_inputs`, `_outputs`, `_references` is not empty.
                //   This is statically safe and will not produce errors at runtime.
                //
                // - Use the reflection API to call empty constructor of a given class.
                //   ContractState::class.java.getConstructor.newInstance()
                //   This is unsafe and may lead to errors at runtime.
                val emptyState = TestContract.TestState(ZKNulls.NULL_ANONYMOUS_PARTY, 0)
                val emptyTxState = TransactionState(emptyState, notary = ZKNulls.NULL_PARTY)
                ZKStateAndRef(emptyTxState, ZKStateRef.empty())
            }
        }
    }
}

fun <T> List<T>.pad(n: Int, default: T) = List(n) {
    if (it < size)
        this[it]
    else {
       default
    }
}
