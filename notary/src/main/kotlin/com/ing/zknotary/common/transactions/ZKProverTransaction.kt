package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateAndRef
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class ZKProverTransaction(
    val inputs: List<ZKStateAndRef<ContractState>>,
    val outputs: List<ZKStateAndRef<ContractState>>,
    val references: List<ZKStateAndRef<ContractState>>,
    val commands: List<Command<*>>,
    val notary: Party?,
    val timeWindow: TimeWindow?,
    val privacySalt: PrivacySalt,
    val networkParametersHash: SecureHash?,
    val attachments: List<AttachmentId>,

    /**
     * Used for serialization of the merkle tree leaves and for ZKStateRefs
     */
    val serializationFactoryService: SerializationFactoryService,

    val componentGroupLeafDigestService: DigestService,
    val nodeDigestService: DigestService = componentGroupLeafDigestService
) : NamedByZKMerkleTree {

    val id by lazy { merkleTree.root }

    /** This additional merkle root is represented by the root hash of a Merkle tree over the transaction components. */
    override val merkleTree by lazy {
        ZKMerkleTree(
            this,
            serializationFactoryService = serializationFactoryService,
            componentGroupLeafDigestService = componentGroupLeafDigestService,
            nodeDigestService = nodeDigestService
        )
    }

    override fun hashCode(): Int = id.hashCode()
    override fun toString() = prettyPrint()
    override fun equals(other: Any?) = if (other !is ZKProverTransaction) false else (this.id == other.id)
}
