package com.ing.zkflow.common.transactions

import net.corda.core.CordaException
import net.corda.core.KeepForDJVM
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.FilteredTransaction

/** Thrown when [FilteredTransaction.verify] fails.
 * @param id transaction's id.
 * @param reason information about the exception.
 */
@KeepForDJVM
@CordaSerializable
class ZKVerifierTransactionVerificationException(val id: SecureHash, val reason: String) : CordaException("Transaction with id:$id cannot be verified. Reason: $reason")
