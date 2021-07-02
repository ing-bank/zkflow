@file:Suppress("FunctionName", "FunctionNaming", "FunctionParameterNaming", "LongParameterList", "TooManypublic functions") // Copy of Corda API

package com.ing.zknotary.testing.dsl

import net.corda.core.DoNotImplement
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.internal.uncheckedCast
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import java.io.InputStream

/**
 * This interface defines output state lookup by label. It is split from the interpreter interfaces so that outputs may
 * be looked up both in ledger{..} and transaction{..} blocks.
 */
@DoNotImplement
public interface OutputStateLookup {
    /**
     * Retrieves an output previously defined by [TransactionDSLInterpreter.output] with a label passed in.
     * @param clazz The class object holding the type of the output state expected.
     * @param label The label of the to-be-retrieved output state.
     * @return The output [StateAndRef].
     */
    public fun <S : ContractState> retrieveOutputStateAndRef(clazz: Class<S>, label: String): StateAndRef<S>
}

/**
 * The different verification modes available for Zinc circuits.
 */
public enum class VerificationMode {
    /**
     * Indicates to use the `run` command to verify a circuit when available, for faster execution.
     */
    RUN,

    /**
     * Indicates to use the `prove` and `verify` commands to verify a circuit.
     */
    PROVE_AND_VERIFY
}

/**
 * This interface asserts that the DSL at hand is capable of verifying its underlying construct(ledger/transaction).
 */
@DoNotImplement
public interface Verifies {
    /**
     * Verifies the ledger/transaction, throws if the verification fails.
     * @param mode The [VerificationMode] to use for verification.
     */
    public fun verifies(mode: VerificationMode = VerificationMode.RUN): EnforceVerifyOrFail

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

/**
 * This interface defines the bare bone public functionality that a Ledger DSL interpreter should implement.
 *
 * TODO (Kotlin 1.1): Use type synonyms to make the type params less unwieldy
 */
@DoNotImplement
public interface LedgerDSLInterpreter<out T : TransactionDSLInterpreter> : Verifies, OutputStateLookup {
    /**
     * Creates and adds a transaction to the ledger.
     * @param transactionLabel Optional label of the transaction, to be used in diagnostic messages.
     * @param transactionBuilder The base transactionBuilder that will be used to build the transaction.
     * @param dsl The dsl that should be interpreted for building the transaction.
     * @return The final [WireTransaction] of the built transaction.
     */
    public fun _transaction(
        transactionLabel: String?,
        transactionBuilder: TransactionBuilder,
        dsl: T.() -> EnforceVerifyOrFail
    ): WireTransaction

    /**
     * Creates and adds a transaction to the ledger.
     * @param transactionLabel Optional label of the transaction, to be used in diagnostic messages.
     * @param transactionBuilder The base transactionBuilder that will be used to build the transaction.
     * @param dsl The dsl that should be interpreted for building the transaction.
     * @return The final [WireTransaction] of the built transaction.
     */
    public fun _zkTransaction(
        transactionLabel: String?,
        transactionBuilder: TransactionBuilder,
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
        transactionBuilder: TransactionBuilder,
        dsl: T.() -> Unit
    ): WireTransaction

    /**
     * Creates a local scoped copy of the ledger.
     * @param dsl The ledger DSL to be interpreted using the copy.
     */
    public fun _tweak(dsl: LedgerDSLInterpreter<T>.() -> Unit)

    /**
     * Adds an attachment to the ledger.
     * @param attachment The [InputStream] defining the contents of the attachment.
     * @return The [SecureHash] that identifies the attachment, to be used in transactions.
     */
    public fun attachment(attachment: InputStream): SecureHash
}

/**
 * This is the class that defines the syntactic sugar of the ledger Test DSL and delegates to the contained interpreter,
 * and what is actually used in `ledger { (...) }`. Add convenience public functions here, or if you want to extend the DSL
 * public functionality then first add your primitive to [LedgerDSLInterpreter] and then add the convenience defaults/extension
 * methods here.
 */
public class LedgerDSL<out T : TransactionDSLInterpreter, out L : LedgerDSLInterpreter<T>>(public val interpreter: L, private val notary: Party) :
    LedgerDSLInterpreter<TransactionDSLInterpreter> by interpreter {

    /**
     * Creates and adds a transaction to the ledger.
     */
    @JvmOverloads
    public fun transaction(
        label: String? = null,
        transactionBuilder: TransactionBuilder = TransactionBuilder(notary = notary),
        dsl: TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail
    ): WireTransaction =
        _transaction(label, transactionBuilder) { TransactionDSL(this, notary).dsl() }

    /**
     * Creates and adds a zktransaction to the ledger.
     */
    @JvmOverloads
    public fun zkTransaction(
        label: String? = null,
        transactionBuilder: TransactionBuilder = TransactionBuilder(notary = notary),
        dsl: TransactionDSL<TransactionDSLInterpreter>.() -> EnforceVerifyOrFail
    ): WireTransaction =
        _zkTransaction(label, transactionBuilder) { TransactionDSL(this, notary).dsl() }

    /**
     * Creates and adds a transaction to the ledger that will not be verified by [verifies].
     */
    @JvmOverloads
    public fun unverifiedTransaction(
        label: String? = null,
        transactionBuilder: TransactionBuilder = TransactionBuilder(notary = notary),
        dsl: TransactionDSL<TransactionDSLInterpreter>.() -> Unit
    ): WireTransaction =
        _unverifiedTransaction(label, transactionBuilder) { TransactionDSL(this, notary).dsl() }

    /** Creates a local scoped copy of the ledger. */
    public fun tweak(dsl: LedgerDSL<T, L>.() -> Unit): Unit = _tweak { LedgerDSL<T, L>(uncheckedCast(this), notary).dsl() }

    /**
     * Retrieves an output previously defined by [TransactionDSLInterpreter._output] with a label passed in.
     */
    public inline fun <reified S : ContractState> String.outputStateAndRef(): StateAndRef<S> =
        retrieveOutputStateAndRef(S::class.java, this)

    /**
     * Retrieves the output [TransactionState] based on the label.
     * @see OutputStateLookup.retrieveOutputStateAndRef
     */
    public inline fun <reified S : ContractState> String.output(): S =
        outputStateAndRef<S>().state.data

    /**
     * Retrieves an output previously defined by [TransactionDSLInterpreter._output] with a label passed in.
     */
    public fun <S : ContractState> retrieveOutput(clazz: Class<S>, label: String): S =
        retrieveOutputStateAndRef(clazz, label).state.data
}
