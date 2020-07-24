package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.Result
import net.corda.core.serialization.SingletonSerializeAsToken

object mockProof : Proof(ByteArray(0), ByteArray(0))
object mockZKService : ZKService, SingletonSerializeAsToken() {
    override fun prove(witness: ByteArray): Result<Proof, String> {
        return Result.Success(mockProof)
    }

    override fun verify(proof: Proof): Result<Unit, String> {
        return Result.Success(Unit)
    }
}

// class InputsProof(private val witness: InputsProofWitness) {
//     fun verify(instance: InputsProofInstance) {
//
//         /*
//          * Rule: witness.ptx.inputs[i] contents hashed with nonce should equal instance.prevVtxOutputHashes[i].
//          * This proves that prover did not change the contents of the input states
//          */
//         witness.ptx.inputs.map { it.state }.forEachIndexed { index, input ->
//             @Suppress("UNCHECKED_CAST")
//             input as TransactionState<ZKContractState>
//
//             TestCase.assertEquals(
//                 instance.inputHashes[index],
//                 BLAKE2s256DigestService.hash(instance.inputNonces[index]!!.bytes + input.fingerprint)
//             )
//         }
//
//         /*
//          * Rule: witness.ptx.inputs[i] contents hashed with nonce should equal instance.prevVtxOutputHashes[i].
//          * This proves that prover did not change the contents of the input states
//          */
//         witness.ptx.references.map { it.state }.forEachIndexed { index, reference ->
//             @Suppress("UNCHECKED_CAST")
//             reference as TransactionState<ZKContractState>
//
//             TestCase.assertEquals(
//                 instance.referenceHashes[index],
//                 BLAKE2s256DigestService.hash(instance.referenceNonces[index]!!.bytes + reference.fingerprint)
//             )
//         }
//
//         /*
//          * Rule: The recalculated Merkle root should match the one from the instance vtx.
//          *
//          * In this case on the Corda side, a ZKProverTransaction id is lazily recalculated always. This means it is
//          * always a direct representation of the ptx contents so we don't have to do a recalculation.
//          * On the Zinc side, we will need explicit recalculation based on the witness inputs.
//          *
//          * Here, we simply compare the witness.ptx.id with the instance.currentVtxId.
//          * This proves that the inputs whose contents have been verified to be unchanged, are also part of the vtx
//          * being verified.
//          */
//         TestCase.assertEquals(instance.currentVtxId, witness.ptx.id)
//     }
// }
