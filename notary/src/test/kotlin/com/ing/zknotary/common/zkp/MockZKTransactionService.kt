package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.contracts.ZKContractState
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.serialization.deserialize
import net.corda.core.serialization.serialize

val mockEmptyProof = ByteArray(0)

@CordaService
open class MockZKTransactionService(val serviceHub: AppServiceHub) : ZKTransactionService, SingletonSerializeAsToken() {
    /**
     * This mock version simply returns the serialized witness, so that we can use it in `verify()`
     * to do all the verifications
     */
    override fun prove(witness: Witness): ByteArray {
        return witness.serialize().bytes
    }

    override fun verify(proof: ByteArray, publicInput: PublicInput) {
        // TODO: Make this use the ZincSerializationFactory when it supports deserialization
        // val serializationFactory = ZincSerializationFactory
        val serializationFactory = SerializationFactory.defaultFactory

        // TODO: Make this use the proper witness.
        // val witness = proof.deserialize<Witness>(ZincSerializationFactory)
        // val ptx = serializationFactory.withCurrentContext(AMQP_RPC_CLIENT_CONTEXT) {
        //     proof.deserialize<ZKProverTransaction>()
        // }
        // This assumes that the proof (for testing only) is simply a serialized witness.
        val witness = proof.deserialize<Witness>(serializationFactory)

        // println(witness.transaction.id)
        // println(witness.transaction::class)
        // println(witness.transaction)
        /*
         * Rule 1: The recalculated Merkle root should match the one from the instance vtx.
         *
         * In this case on the Corda side, a ZKProverTransaction id is lazily recalculated always. This means it is
         * always a direct representation of the ptx contents so we don't have to do a recalculation.
         * On the Zinc side, we will need explicit recalculation based on the witness transaction components.
         *
         * Here, we simply compare the witness.ptx.id with the instance.currentVtxId.
         * This proves that the inputs whose contents have been verified to be unchanged, are also part of the vtx
         * being verified.
         */
        if (publicInput.transactionId != witness.transaction.id) error(
            "The calculated Merkle root from the witness (${witness.transaction.id}) does not match " +
                "the expected transaction id from the public input (${publicInput.transactionId})."
        )

        /*
         * Rule 2: witness.ptx.inputs[i] contents hashed with nonce should equal publicInput.prevVtxOutputHashes[i].
         * This proves that prover did not change the contents of the input states
         */
        witness.transaction.inputs.map { it.state }.forEachIndexed { index, state ->
            @Suppress("UNCHECKED_CAST")
            state as TransactionState<ZKContractState>

            val nonceFromWitness = witness.inputNonces.getOrElse(index) {
                error("Nonce not present in public input for input $index of tx ${witness.transaction.id}")
            }

            val leafHashFromPublicInput = publicInput.inputHashes.getOrElse(index) {
                error("Leaf hash not present in public input for input $index of tx ${witness.transaction.id}")
            }

            val calculatedLeafHashFromWitness =
                BLAKE2s256DigestService.hash(nonceFromWitness.bytes + state.fingerprint)

            if (leafHashFromPublicInput != calculatedLeafHashFromWitness) error(
                "Calculated leaf hash ($calculatedLeafHashFromWitness} for input $index of tx ${witness.transaction.id} does " +
                    "not match the leaf hash from the public input ($leafHashFromPublicInput)."
            )
        }

        /*
         * Rule 3: witness.ptx.references[i] contents hashed with nonce should equal publicreference.prevVtxOutputHashes[i].
         * This proves that prover did not change the contents of the reference states
         */
        witness.transaction.references.map { it.state }.forEachIndexed { index, state ->
            @Suppress("UNCHECKED_CAST")
            state as TransactionState<ZKContractState>

            val nonceFromWitness = witness.referenceNonces.getOrElse(index) {
                error("Nonce not present in public reference for reference $index of tx ${witness.transaction.id}")
            }

            val leafHashFromPublicreference = publicInput.referenceHashes.getOrElse(index) {
                error("Leaf hash not present in public reference for reference $index of tx ${witness.transaction.id}")
            }

            val calculatedLeafHashFromWitness =
                BLAKE2s256DigestService.hash(nonceFromWitness.bytes + state.fingerprint)

            if (leafHashFromPublicreference != calculatedLeafHashFromWitness) error(
                "Calculated leaf hash ($calculatedLeafHashFromWitness} for reference $index of tx ${witness.transaction.id} does " +
                    "not match the leaf hash from the public input ($leafHashFromPublicreference)."
            )
        }
    }
}
