package com.ing.zkflow.testing.dsl.interfaces

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.zkp.ZKTransactionService
import net.corda.core.DoNotImplement
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.WireTransaction
import java.io.InputStream

/**
 * This interface defines the bare bone public functionality that a Ledger DSL interpreter should implement.
 *
 * TODO (Kotlin 1.1): Use type synonyms to make the type params less unwieldy
 */
@DoNotImplement
@Suppress("FunctionNaming", "FunctionName") // Copy of Corda API
public interface ZKLedgerDSLInterpreter<out T : ZKTransactionDSLInterpreter> : Verifies, OutputStateLookup {
    public val zkService: ZKTransactionService
    public val zkNetworkParameters: ZKNetworkParameters
    public val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage

    /**
     * Creates and adds a transaction to the ledger.
     * @param transactionLabel Optional label of the transaction, to be used in diagnostic messages.
     * @param transactionBuilder The base transactionBuilder that will be used to build the transaction.
     * @param dsl The dsl that should be interpreted for building the transaction.
     * @return The final [WireTransaction] of the built transaction.
     */
    public fun _transaction(
        transactionLabel: String?,
        transactionBuilder: ZKTransactionBuilder,
        dsl: T.() -> EnforceVerifyOrFail
    ): WireTransaction

    /**
     * Creates and adds a transaction to the ledger that will not be verified by [verifies].
     * @param transactionLabel Optional label of the transaction, to be used in diagnostic messages.
     * @param transactionBuilder The base transactionBuilder that will be used to build the transaction.
     * @param dsl The dsl that should be interpreted for building the transaction.
     * @return The final [WireTransaction] of the built transaction.
     */
    public fun _unverifiedTransaction(
        transactionLabel: String?,
        transactionBuilder: ZKTransactionBuilder,
        dsl: T.() -> Unit
    ): WireTransaction

    /**
     * Creates a local scoped copy of the ledger.
     * @param dsl The ledger DSL to be interpreted using the copy.
     */
    public fun _tweak(dsl: ZKLedgerDSLInterpreter<T>.() -> Unit)

    /**
     * Adds an attachment to the ledger.
     * @param attachment The [InputStream] defining the contents of the attachment.
     * @return The [SecureHash] that identifies the attachment, to be used in transactions.
     */
    public fun attachment(attachment: InputStream): SecureHash
}
