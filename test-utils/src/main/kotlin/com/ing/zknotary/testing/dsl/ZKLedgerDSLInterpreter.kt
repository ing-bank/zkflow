@file:Suppress("FunctionName", "FunctionNaming", "FunctionParameterNaming", "LongParameterList", "TooManyFunctions") // Copy of Corda API

package com.ing.zknotary.testing.dsl

import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.transactions.ZKTransactionBuilder
import net.corda.core.DoNotImplement
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.uncheckedCast
import net.corda.core.transactions.WireTransaction
import net.corda.testing.dsl.LedgerDSLInterpreter
import net.corda.testing.dsl.OutputStateLookup
import net.corda.testing.dsl.TransactionDSLInterpreter
import java.io.InputStream

/**
 * This interface defines output state lookup by label. It is split from the interpreter interfaces so that outputs may
 * be looked up both in ledger{..} and transaction{..} blocks.
 */
@DoNotImplement
public interface ZKOutputStateLookup {
    /**
     * Retrieves an output previously defined by [TransactionDSLInterpreter.output] with a label passed in.
     * @param clazz The class object holding the type of the output state expected.
     * @param label The label of the to-be-retrieved output state.
     * @return The output [StateAndRef].
     */
    public fun <S : ZKContractState> retrieveOutputStateAndRef(clazz: Class<S>, label: String): StateAndRef<S>
}

@DoNotImplement
public interface ZKVerifies {
    /**
     * Verifies the ledger/transaction, throws if the verification fails.
     */
    public fun verifies(): EnforceVerifyOrFail

    /**
     * Asserts that verifies() throws.
     * @param expectedMessage An optional string to be searched for in the raised exception.
     */
    public fun failsWith(expectedMessage: String?): EnforceVerifyOrFail {
        val exceptionThrown = try {
            verifies()
            false
        } catch (exception: Exception) {
            if (expectedMessage != null) {
                val exceptionMessage = exception.message
                if (exceptionMessage == null) {
                    throw AssertionError(
                        "Expected exception containing '$expectedMessage' but raised exception had no message",
                        exception
                    )
                } else if (!exceptionMessage.toLowerCase().contains(expectedMessage.toLowerCase())) {
                    throw AssertionError(
                        "Expected exception containing '$expectedMessage' but raised exception was '$exception'",
                        exception
                    )
                }
            }
            true
        }

        if (!exceptionThrown) {
            throw AssertionError("Expected exception but didn't get one")
        }

        return EnforceVerifyOrFail.Token
    }

    /**
     * Asserts that [verifies] throws, with no condition on the exception message.
     */
    public fun fails(): EnforceVerifyOrFail = failsWith(null)

    /**
     * @see failsWith
     */
    public infix fun `fails with`(msg: String): EnforceVerifyOrFail = failsWith(msg)
}

@DoNotImplement
public interface ZKLedgerDSLInterpreter<out T : ZKTransactionDSLInterpreter> : ZKVerifies, ZKOutputStateLookup {
    /**
     * Creates and adds a transaction to the ledger.
     * @param transactionLabel Optional label of the transaction, to be used in diagnostic messages.
     * @param ZKTransactionBuilder The base ZKTransactionBuilder that will be used to build the transaction.
     * @param dsl The dsl that should be interpreted for building the transaction.
     * @return The final [WireTransaction] of the built transaction.
     */
    public fun _transaction(
        transactionLabel: String?,
        ZKTransactionBuilder: ZKTransactionBuilder,
        dsl: T.() -> EnforceVerifyOrFail
    ): WireTransaction

    /**
     * Creates and adds a transaction to the ledger that will not be verified by [verifies].
     * @param transactionLabel Optional label of the transaction, to be used in diagnostic messages.
     * @param ZKTransactionBuilder The base ZKTransactionBuilder that will be used to build the transaction.
     * @param dsl The dsl that should be interpreted for building the transaction.
     * @return The final [WireTransaction] of the built transaction.
     */
    public fun _unverifiedTransaction(
        transactionLabel: String?,
        ZKTransactionBuilder: ZKTransactionBuilder,
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

/**
 * This is the class that defines the syntactic sugar of the ledger Test DSL and delegates to the contained interpreter,
 * and what is actually used in `ledger { (...) }`. Add convenience functions here, or if you want to extend the DSL
 * functionality then first add your primitive to [LedgerDSLInterpreter] and then add the convenience defaults/extension
 * methods here.
 */
public class ZKLedgerDSL<out T : ZKTransactionDSLInterpreter, out L : ZKLedgerDSLInterpreter<T>>(
    public val interpreter: L,
    private val notary: Party
) :
    ZKLedgerDSLInterpreter<ZKTransactionDSLInterpreter> by interpreter {

    /**
     * Creates and adds a transaction to the ledger.
     */
    @JvmOverloads
    public fun transaction(
        label: String? = null,
        ZKTransactionBuilder: ZKTransactionBuilder = ZKTransactionBuilder(notary = notary),
        dsl: ZKTransactionDSL<ZKTransactionDSLInterpreter>.() -> EnforceVerifyOrFail
    ): WireTransaction =
        _transaction(label, ZKTransactionBuilder) { ZKTransactionDSL(this, notary).dsl() }

    /**
     * Creates and adds a transaction to the ledger that will not be verified by [verifies].
     */
    @JvmOverloads
    public fun unverifiedTransaction(
        label: String? = null,
        ZKTransactionBuilder: ZKTransactionBuilder = ZKTransactionBuilder(notary = notary),
        dsl: ZKTransactionDSL<ZKTransactionDSLInterpreter>.() -> Unit
    ): WireTransaction =
        _unverifiedTransaction(label, ZKTransactionBuilder) { ZKTransactionDSL(this, notary).dsl() }

    /** Creates a local scoped copy of the ledger. */
    public fun tweak(dsl: ZKLedgerDSL<T, L>.() -> Unit): Unit = _tweak { ZKLedgerDSL<T, L>(uncheckedCast(this), notary).dsl() }

    /**
     * Retrieves an output previously defined by [TransactionDSLInterpreter._output] with a label passed in.
     */
    public inline fun <reified S : ZKContractState> String.outputStateAndRef(): StateAndRef<S> =
        retrieveOutputStateAndRef(S::class.java, this)

    /**
     * Retrieves the output [TransactionState] based on the label.
     * @see OutputStateLookup.retrieveOutputStateAndRef
     */
    public inline fun <reified S : ZKContractState> String.output(): S =
        outputStateAndRef<S>().state.data

    /**
     * Retrieves an output previously defined by [TransactionDSLInterpreter._output] with a label passed in.
     */
    public fun <S : ZKContractState> retrieveOutput(clazz: Class<S>, label: String): S =
        retrieveOutputStateAndRef(clazz, label).state.data
}
