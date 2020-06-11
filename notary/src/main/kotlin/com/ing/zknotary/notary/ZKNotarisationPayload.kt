package com.ing.zknotary.notary

import com.ing.zknotary.common.transactions.ZKVerifierTransaction
import net.corda.core.flows.NotarisationRequestSignature
import net.corda.core.serialization.CordaSerializable

/**
 * Container for the transaction and notarisation request signature.
 * This is the payload that gets sent by a client to a notary service for committing the input states of the [transaction].
 */
@CordaSerializable
data class ZKNotarisationPayload(
    val transaction: ZKVerifierTransaction,
    val requestSignature: NotarisationRequestSignature
)
