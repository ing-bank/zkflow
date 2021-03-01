//package com.ing.zknotary.common.transactions
//
//import com.ing.zknotary.common.contracts.ZKCommandData
//import com.ing.zknotary.common.crypto.BLAKE2S256
//import com.ing.zknotary.common.util.ComponentPaddingConfiguration
//import com.ing.zknotary.common.util.PaddingWrapper
//import net.corda.core.contracts.Command
//import net.corda.core.contracts.ComponentGroupEnum
//import net.corda.core.contracts.ContractState
//import net.corda.core.contracts.PrivacySalt
//import net.corda.core.contracts.StateAndRef
//import net.corda.core.contracts.TimeWindow
//import net.corda.core.contracts.TransactionState
//import net.corda.core.crypto.DigestService
//import net.corda.core.crypto.SecureHash
//import net.corda.core.crypto.algorithm
//import net.corda.core.identity.Party
//import net.corda.core.node.services.AttachmentId
//import net.corda.core.serialization.CordaSerializable
//import java.security.PublicKey
//import java.time.Instant
//
//@Suppress("LongParameterList")
//@CordaSerializable
//class ZKProverTransactionXXX internal constructor(
//    val inputs: List<StateAndRef<ContractState>>,
//    val outputs: List<TransactionState<ContractState>>,
//    val references: List<StateAndRef<ContractState>>,
//    val command: Command<ZKCommandData>,
//    val notary: Party,
//    val timeWindow: TimeWindow?,
//
//    /**
//     *  For the ZKProverTransaction to be deterministically created from a LedgerTransaction,
//     *  this needs to always be the privacySalt from the LedgerTransaction
//     */
//    val privacySalt: PrivacySalt,
//
//    /**
//     * Decide how to handle the networkparameters. Simplest it to use the hash only
//     * since the verifier will also have the hash, but we will need to possibly use the contents for verification logic?
//     * The non-validating notary receives the hash as part of the normal ftx and checks the platform version and notary.
//     * For now, we will not need the other parameters for verification yet. When we do, we will need to supply both the
//     * Parameters and their hash to the verification circuit and ensure that they are the same. Then the hash is part of
//     * the instance, so the verifier is convinced that all is linked. Then we can use the parameters in verification logic.
//     * In standard Corda, the hash is: SHA256(cordaSerialize(NetworkParameters)), where cordaSerialize is the standard Corda
//     * AMQP serialization.
//     */
//    val networkParametersHash: SecureHash?,
//
//    // For now we ignore attachment contents inside the circuit. We might want to use them for attaching some circuit identifier or even the verifier key.
//    val attachments: List<AttachmentId>,
//
//    val componentGroupLeafDigestService: DigestService,
//    val nodeDigestService: DigestService
//) : NamedByZKMerkleTree {
//    val componentPaddingConfiguration = command.value.paddingConfiguration
//
//    val padded = Padded(
//        originalInputs = inputs,
//        originalOutputs = outputs,
//        originalReferences = references,
//        originalSigners = command.signers,
//        originalAttachments = attachments,
//        originalTimeWindow = timeWindow,
//        originalNetworkParametersHash = networkParametersHash,
//        paddingConfiguration = componentPaddingConfiguration
//    )
//
//    init {
//        componentPaddingConfiguration.validate(this)
//    }
//
//    override val id by lazy { merkleTree.root }
//
//    /** This additional merkle root is represented by the root hash of a Merkle tree over the transaction components. */
//    override val merkleTree by lazy {
//        ZKFullMerkleTree(this)
//    }
//
//    override fun hashCode(): Int = id.hashCode()
//    override fun toString() = prettyPrint()
//    override fun equals(other: Any?) = if (other !is ZKProverTransaction) false else (this.id == other.id)
//
//    data class Padded(
//        private val originalInputs: List<StateAndRef<ContractState>>,
//        private val originalOutputs: List<TransactionState<ContractState>>,
//        private val originalSigners: List<PublicKey>,
//        private val originalReferences: List<StateAndRef<ContractState>>,
//        private val originalAttachments: List<AttachmentId>,
//        private val originalTimeWindow: TimeWindow?,
//        private val originalNetworkParametersHash: SecureHash?,
//        val paddingConfiguration: ComponentPaddingConfiguration
//    ) {
//
//        fun inputs(): List<PaddingWrapper<StateAndRef<ContractState>>> {
//            val filler = filler(ComponentGroupEnum.INPUTS_GROUP)
//            require(filler is ComponentPaddingConfiguration.Filler.StateAndRef) { "Expected filler of type StateAndRef" }
//            return originalInputs.wrappedPad(sizeOf(ComponentGroupEnum.INPUTS_GROUP), filler.content)
//        }
//
//        fun outputs(): List<PaddingWrapper<TransactionState<ContractState>>> {
//            val filler = filler(ComponentGroupEnum.OUTPUTS_GROUP)
//            require(filler is ComponentPaddingConfiguration.Filler.TransactionState) { "Expected filler of type TransactionState" }
//            return originalOutputs.wrappedPad(sizeOf(ComponentGroupEnum.OUTPUTS_GROUP), filler.content)
//        }
//
//        fun references(): List<PaddingWrapper<StateAndRef<ContractState>>> {
//            val filler = filler(ComponentGroupEnum.REFERENCES_GROUP)
//            require(filler is ComponentPaddingConfiguration.Filler.StateAndRef) { "Expected filler of type StateAndRef" }
//            return originalReferences.wrappedPad(sizeOf(ComponentGroupEnum.REFERENCES_GROUP), filler.content)
//        }
//
//        fun attachments(): List<PaddingWrapper<SecureHash>> {
//            val filler = filler(ComponentGroupEnum.ATTACHMENTS_GROUP)
//            require(filler is ComponentPaddingConfiguration.Filler.SecureHash) { "Expected filler of type SecureHash" }
//            return originalAttachments.wrappedPad(sizeOf(ComponentGroupEnum.ATTACHMENTS_GROUP), filler.content)
//        }
//
//        fun timeWindow() = originalTimeWindow.wrappedPad(TimeWindow.fromOnly(Instant.MIN))
//
//        fun networkParametersHash(): PaddingWrapper<SecureHash> {
//            val zeroHash =
//                if (originalNetworkParametersHash == null) {
//                    SecureHash.zeroHashFor(SecureHash.BLAKE2S256)
//                } else {
//                    SecureHash.zeroHashFor(originalNetworkParametersHash.algorithm)
//                }
//            return originalNetworkParametersHash.wrappedPad(zeroHash)
//        }
//
//        fun signers(): List<PaddingWrapper<PublicKey>> {
//            val filler = filler(ComponentGroupEnum.SIGNERS_GROUP)
//            require(filler is ComponentPaddingConfiguration.Filler.PublicKey) { "Expected filler of type PublicKey" }
//            return originalSigners.wrappedPad(sizeOf(ComponentGroupEnum.SIGNERS_GROUP), filler.content)
//        }
//
//        /**
//         * Returns appropriate size or fail.
//         */
//        fun sizeOf(componentGroup: ComponentGroupEnum) =
//            paddingConfiguration.sizeOf(componentGroup) ?: error("Expected a positive number")
//
//        /**
//         * Returns appropriate size or fail.
//         */
//        fun filler(componentGroup: ComponentGroupEnum) =
//            paddingConfiguration.filler(componentGroup) ?: error("Expected a filler object")
//    }
//}
