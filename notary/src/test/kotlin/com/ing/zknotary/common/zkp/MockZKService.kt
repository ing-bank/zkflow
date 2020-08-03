package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.transactions.ZKProverTransaction
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.serialization.deserialize

val mockEmptyProof = ByteArray(0)

@CordaService
open class MockZKService(val serviceHub: AppServiceHub) : ZKService, SingletonSerializeAsToken() {
    /**
     * This mock version simply returns the witness, so that we can use it in `verify()`
     * to do all the verifications
     */
    override fun prove(witness: ByteArray): ByteArray {
        return witness
    }

    override fun verify(proof: ByteArray, publicInput: ByteArray) {
        // TODO: Make this use the ZincSerializationFactory when it supports deserialization
        // val serializationFactory = ZincSerializationFactory
        val serializationFactory = SerializationFactory.defaultFactory

        // TODO: Make this use the proper witness.
        // val witness = proof.deserialize<Witness>(ZincSerializationFactory)
        // val ptx = serializationFactory.withCurrentContext(AMQP_RPC_CLIENT_CONTEXT) {
        //     proof.deserialize<ZKProverTransaction>()
        // }
        val ptx = proof.deserialize<ZKProverTransaction>(serializationFactory)
        val witness = Witness(ptx, emptyList())

        val instance = publicInput.deserialize<PublicInput>(serializationFactory)

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
        if (instance.transactionId != witness.ptx.id) error(
            "The calculated Merkle root from the witness (${witness.ptx.id}) does not match " +
                "the transaction id from the public input (${instance.transactionId})."
        )

        /*
         * Rule 2: witness.ptx.inputs[i] contents hashed with nonce should equal instance.prevVtxOutputHashes[i].
         * This proves that prover did not change the contents of the input states
         */
        witness.ptx.inputs.map { it.state }.forEachIndexed { index, input ->
            @Suppress("UNCHECKED_CAST")
            input as TransactionState<ZKContractState>

            val nonceFromPublicInput = instance.inputNonces[index]
                ?: error("Nonce not present in public input for input $index of tx ${witness.ptx.id}")
            val leafHashFromPublicInput = instance.inputHashes[index]
                ?: error("Leaf hash not present in public input for input $index of tx ${witness.ptx.id}")

            val calculatedLeafHashFromWitness =
                BLAKE2s256DigestService.hash(nonceFromPublicInput.bytes + input.fingerprint)

            if (leafHashFromPublicInput != calculatedLeafHashFromWitness) error(
                "Calculated leaf hash ($calculatedLeafHashFromWitness} for input $index of tx ${witness.ptx.id} does " +
                    "not match the leaf hash from the public input ($leafHashFromPublicInput)."
            )
        }
    }
}
