package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.states.toZKStateAndRef
import net.corda.core.DeleteForDJVM
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
    override val merkleTree: ZKMerkleTree by lazy {
        ZKMerkleTree(
            this,
            serializationFactoryService = serializationFactoryService,
            componentGroupLeafDigestService = componentGroupLeafDigestService,
            nodeDigestService = nodeDigestService
        )
    }

    /**
     * Build filtered transaction using provided filtering functions.
     */
    fun toZKVerifierTransaction(filtering: Predicate<Any>): ZKVerifierTransaction =
        ZKVerifierTransaction.fromZKProverTransaction(this, filtering)

    override fun equals(other: Any?): Boolean {
        if (other is ZKProverTransaction) {
            return (this.id == other.id)
        }
        return false
    }

    override fun hashCode(): Int = id.hashCode()

    @DeleteForDJVM
    override fun toString(): String {
        val buf = StringBuilder()
        buf.appendln("Transaction:")

        fun addComponentList(buf: StringBuilder, name: String, componentList: List<*>) {
            if (componentList.isNotEmpty()) buf.appendln(" - $name:")
            for ((index, component) in componentList.withIndex()) {
                buf.appendln("\t[$index]:\t$component")
            }
        }

        addComponentList(buf, "REFS", references)
        addComponentList(buf, "INPUTS", inputs)
        addComponentList(buf, "OUTPUTS", outputs)
        addComponentList(buf, "COMMANDS", commands)
        addComponentList(buf, "ATTACHMENT HASHES", attachments)

        if (networkParametersHash != null) {
            buf.appendln(" - PARAMETERS HASH:  $networkParametersHash")
        }
        return buf.toString()
    }
}
