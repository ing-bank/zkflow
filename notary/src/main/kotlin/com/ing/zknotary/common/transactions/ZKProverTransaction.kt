package com.ing.zknotary.common.transactions

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.states.toZKStateAndRef
import java.util.function.Predicate
import net.corda.core.DeleteForDJVM
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction

/**
 * This should also include signatures
 */
class ZKProverTransaction(
    val inputs: List<ZKStateAndRef<ContractState>>,
    val outputs: List<ZKStateAndRef<ContractState>>,
    val references: List<ZKStateAndRef<ContractState>>,
    val commands: List<Command<*>>,
    val notary: Party?,
    val timeWindow: TimeWindow?,
    val privacySalt: PrivacySalt,
    val networkParametersHash: SecureHash?,
    val attachments: List<SecureHash>,

    /**
     * Used for serialization of the merkle tree leaves
     */
    val serializationFactoryService: SerializationFactoryService,

    /**
     * Digest services for the merkle tree
     * TODO: split this into two:componentGroupLeafDigestService and nodeDigestService
     */
    val digestService: DigestService
) : NamedByZKMerkleTree {

    constructor(
        ltx: LedgerTransaction,
        serializationFactoryService: SerializationFactoryService,
        digestService: DigestService
    ) : this(
        inputs = ltx.inputs.map { it.toZKStateAndRef(serializationFactoryService, digestService) },
        outputs = ltx.outputs.map { it.toZKStateAndRef(serializationFactoryService, digestService) },
        references = ltx.references.map { it.toZKStateAndRef(serializationFactoryService, digestService) },
        commands = ltx.commands.map { Command(it.value, it.signers) },
        notary = ltx.notary,
        timeWindow = ltx.timeWindow,
        privacySalt = ltx.privacySalt,

        // Decide how to handle the networkparameters. Simplest it to use the hash only
        // since the verifier will also have the hash, but we will need to possibly use the contents for verification logic?
        // The non-validating notary receives the hash as part of the normal ftx and checks the platform version and notary.
        // For now, we will not need the other parameters for verification yet. When we do, we will need to supply both the
        // Parameters and their hash to the verification circuit and ensure that they are the same. Then the hash is part of
        // the instance, so the verifier is convinced that all is linked. Then we can use the parameters in verification logic.
        // In standard Corda, the hash is: SHA256(cordaSerialize(NetworkParameters)), where cordaSerialize is the standard Corda
        // AMQP serialization.
        networkParametersHash = ltx.networkParameters?.serialize()?.hash,

        // For now we ignore attachments, but we might want to use them later for attaching some circuit identifier or even the verifier key.
        // Then we can make sure that if the tx verifies with that key, all is well?
        attachments = ltx.attachments.map { it.id },
        serializationFactoryService = serializationFactoryService,
        digestService = digestService
    )

    val id by lazy { merkleTree.root }

    /** This additional merkle root is represented by the root hash of a Merkle tree over the transaction components. */
    override val merkleTree: ZKMerkleTree by lazy {
        ZKMerkleTree(
            this,
            serializationFactoryService = serializationFactoryService,
            componentGroupLeafDigestService = digestService,
            nodeDigestService = digestService
        )
    }

    /**
     * Build filtered transaction using provided filtering functions.
     */
    fun buildVerifierTransaction(filtering: Predicate<Any>): ZKVerifierTransaction =
        ZKVerifierTransaction.buildFilteredTransaction(this, filtering)

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
