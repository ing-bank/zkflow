package com.ing.zknotary.notary

import com.ing.zknotary.common.states.ZKStateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.NotarisationRequestSignature
import net.corda.core.flows.NotaryError
import net.corda.core.identity.Party
import net.corda.core.internal.notary.NotaryInternalException
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.serialize
import net.corda.core.transactions.CoreTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.toBase58String

/**
 * A notarisation request specifies a list of states to consume and the id of the consuming transaction. Its primary
 * purpose is for notarisation traceability – a signature over the notarisation request, [NotarisationRequestSignature],
 * allows a notary to prove that a certain party requested the consumption of a particular state.
 *
 * While the signature must be retained, the notarisation request does not need to be transferred or stored anywhere - it
 * can be built from a [SignedTransaction] or a [CoreTransaction]. The notary can recompute it from the committed states index.
 *
 * Reference inputs states are not included as a separate property in the [ZKNotarisationRequest] as they are not
 * consumed.
 *
 * In case there is a need to prove that a party spent a particular state, the notary will:
 * 1) Locate the consuming transaction id in the index, along with all other states consumed in the same transaction.
 * 2) Build a [ZKNotarisationRequest].
 * 3) Locate the [NotarisationRequestSignature] for the transaction id. The signature will contain the signing public key.
 * 4) Demonstrate the signature verifies against the serialized request. The provided states are always sorted internally,
 *    to ensure the serialization does not get affected by the order.
 */
@CordaSerializable
// TODO: should this also contain the states that will be created?
class ZKNotarisationRequest(statesToConsume: List<ZKStateRef>, val transactionId: SecureHash) {
    companion object {
        /** Sorts in ascending order first by transaction hash, then by output index. */
        private val stateRefComparator = compareBy<ZKStateRef> { it.id }
    }

    private val _statesToConsumeSorted = statesToConsume.sortedWith(stateRefComparator)

    /** States this request specifies to be consumed. Sorted to ensure the serialized form does not get affected by the state order. */
    val statesToConsume: List<ZKStateRef> get() = _statesToConsumeSorted // Getter required for AMQP serialization

    /** Verifies the signature against this notarisation request. Checks that the signature is issued by the right party. */
    fun verifySignature(requestSignature: NotarisationRequestSignature, intendedSigner: Party) {
        try {
            val signature = requestSignature.digitalSignature
            require(intendedSigner.owningKey == signature.by) {
                "Expected a signature by ${intendedSigner.owningKey.toBase58String()}, but received by ${signature.by.toBase58String()}}"
            }

            // TODO: if requestSignature was generated over an old version of NotarisationRequest, we need to be able to
            // reserialize it in that version to get the exact same bytes. Modify the serialization logic once that's
            // available.
            val expectedSignedBytes = this.serialize().bytes
            signature.verify(expectedSignedBytes)
        } catch (e: Exception) {
            val error = NotaryError.RequestSignatureInvalid(e)
            throw NotaryInternalException(error)
        }
    }
}
