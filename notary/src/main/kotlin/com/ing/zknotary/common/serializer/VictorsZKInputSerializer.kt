package com.ing.zknotary.common.serializer

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.serialize
import net.corda.core.transactions.LedgerTransaction

object VictorsZKInputSerializer : ZKInputSerializer {
    // FIXME: should be turned into proper serialization of any tx generic data structure
    override fun serializeWitness(tx: LedgerTransaction, signatures: List<ByteArray>): ByteArray {
        var witness = ByteArray(0) // Or perhaps this should be JSON?

        /**
         * We keep the same order as [ComponentGroupEnum]
         * INPUTS_GROUP, // ordinal = 0.
         * OUTPUTS_GROUP, // ordinal = 1.
         * COMMANDS_GROUP, // ordinal = 2.
         * ATTACHMENTS_GROUP, // ordinal = 3.
         * NOTARY_GROUP, // ordinal = 4.
         * TIMEWINDOW_GROUP, // ordinal = 5.
         * SIGNERS_GROUP, // ordinal = 6.
         * REFERENCES_GROUP, // ordinal = 7.
         * PARAMETERS_GROUP // ordinal = 8.
         */
        witness += serializeInputs(tx)
        witness += serializeOutputs(tx)
        witness += serializeCommandData(
            tx
        ) // Note that the Commands in a tx are made up out of two component groups in the Merkle tree: CommandData and commandSigners. They are serialized serparately.
        // We will skip the attachments and only use its component group hash for merkle root recalculation
        witness += serializeNotary(tx) // We don't need to validate that this is the correct notary as the NotaryServiceFlow already does this. But we might need it for other checks
        witness += serializeTimeWindow(tx) // The TimeWindow is committed by the FilteredTransaction.verify, but we may still need it for business logic.
        witness += serializeSigners(tx) // // Note that the Commands in a tx are made up out of two component groups in the Merkle tree: CommandData and commandSigners. They are serialized serparately.
        witness += serializeReferenceStates(
            tx
        )
        // We will skip the network parameters group and only use its component group hash for merkle root calculation

        // Other components we need
        witness += serializeSignatures(
            signatures
        )
        witness += serializePrivacySalt(
            tx
        )
        witness += serializeComponentGroupHashes(
            tx
        )

        return witness
    }

    private fun serializeSigners(tx: LedgerTransaction): ByteArray {
        return ByteArray(0)
    }

    private fun serializeTimeWindow(tx: LedgerTransaction): ByteArray {
        return ByteArray(0)
    }

    private fun serializeNotary(tx: LedgerTransaction): ByteArray {
        return ByteArray(0)
    }

    private fun serializeComponentGroupHashes(tx: LedgerTransaction): ByteArray {
        // FIXME: This is impossible with a LedgerTransaction, unless we recalculate them here. We need a TraversableTransaction for this
        return ByteArray(0)
    }

    private fun serializePrivacySalt(tx: LedgerTransaction): ByteArray {
        // return tx.privacySalt.bytes
        return ByteArray(0)
    }

    private fun serializeReferenceStates(tx: LedgerTransaction): ByteArray {
        return ByteArray(0)
    }

    private fun serializeSignatures(signatures: List<ByteArray>): ByteArray {
        // return signatures.reduce { acc, sig -> acc + sig // 64 bytes per sig } }
        return ByteArray(0)
    }

    private fun serializeCommandData(tx: LedgerTransaction): ByteArray {
        // As an example if not using Corda serialization: how to extract meaningful data from a Corda data structure:
        // val commandSigners = tx.commands.flatMap { command -> command.signers }
        // commandSigners.forEach { pubkey ->
        //     pubkey as EdDSAPublicKey
        //     witness += pubkey.abyte // 32 bytes
        // }
        return ByteArray(0)
    }

    private fun serializeOutputs(tx: LedgerTransaction): ByteArray {
        return ByteArray(0)
    }

    private fun serializeInputs(tx: LedgerTransaction): ByteArray {
        // return ByteArray(0)
        // For testing, only serialize one input and nothing else for the entire tx. Lets see if we can deserialize that in Zinc
        return tx.inputStates[0].serialize().bytes
    }

    /**
     * This seems overkill now, but later we will add more things to the instance
     */
    override fun serializeInstance(zkTransactionId: SecureHash): ByteArray {
        return zkTransactionId.bytes // These are the raw bytes of the the transaction id hash (merkle root)
    }
}