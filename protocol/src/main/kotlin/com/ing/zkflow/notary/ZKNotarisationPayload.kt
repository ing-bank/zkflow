package com.ing.zkflow.notary

import com.ing.zkflow.common.transactions.SignedZKVerifierTransaction
import net.corda.core.flows.NotarisationRequestSignature
import net.corda.core.serialization.CordaSerializable

/**
 * Container for the transaction and notarisation request signature.
 * This is the payload that gets sent by a client to a notary service for committing the input states of the [transaction].
 */
@CordaSerializable
data class ZKNotarisationPayload(
    val transaction: SignedZKVerifierTransaction,
    val requestSignature: NotarisationRequestSignature
)
