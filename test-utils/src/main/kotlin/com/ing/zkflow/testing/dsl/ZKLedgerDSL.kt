package com.ing.zkflow.testing.dsl

import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.testing.dsl.interfaces.EnforceVerifyOrFail
import com.ing.zkflow.testing.dsl.interfaces.ZKLedgerDSLInterpreter
import com.ing.zkflow.testing.dsl.interfaces.ZKTransactionDSLInterpreter
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.internal.uncheckedCast
import net.corda.core.transactions.WireTransaction

/**
 * This is the class that defines the syntactic sugar of the ledger Test DSL and delegates to the contained interpreter,
 * and what is actually used in `ledger { (...) }`. Add convenience public functions here, or if you want to extend the DSL
 * public functionality then first add your primitive to [ZKLedgerDSLInterpreter] and then add the convenience defaults/extension
 * methods here.
 */
public class ZKLedgerDSL<out T : ZKTransactionDSLInterpreter, out L : ZKLedgerDSLInterpreter<T>>(
    private val interpreter: L,
    private val notary: Party,
    override val zkNetworkParameters: ZKNetworkParameters,
) : ZKLedgerDSLInterpreter<ZKTransactionDSLInterpreter> by interpreter {

    /**
     * Creates and adds a transaction to the ledger.
     */
    @JvmOverloads
    public fun transaction(
        label: String? = null,
        transactionBuilder: ZKTransactionBuilder = ZKTransactionBuilder(notary, zkNetworkParameters),
        dsl: ZKTransactionDSL<ZKTransactionDSLInterpreter>.() -> EnforceVerifyOrFail
    ): WireTransaction =
        _transaction(label, transactionBuilder) { ZKTransactionDSL(this, notary).dsl() }

    /**
     * Creates and adds a transaction to the ledger that will not be verified by [verifies].
     */
    @JvmOverloads
    public fun unverifiedTransaction(
        label: String? = null,
        transactionBuilder: ZKTransactionBuilder = ZKTransactionBuilder(notary, zkNetworkParameters),
        dsl: ZKTransactionDSL<ZKTransactionDSLInterpreter>.() -> Unit
    ): WireTransaction =
        _unverifiedTransaction(label, transactionBuilder) { ZKTransactionDSL(this, notary).dsl() }

    /** Creates a local scoped copy of the ledger. */
    public fun tweak(dsl: ZKLedgerDSL<T, L>.() -> Unit): Unit =
        _tweak { ZKLedgerDSL<T, L>(uncheckedCast(this), notary, zkNetworkParameters).dsl() }

    /**
     * Retrieves an output previously defined by [ZKTransactionDSLInterpreter._output] with a label passed in.
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
     * Retrieves an output previously defined by [ZKTransactionDSLInterpreter._output] with a label passed in.
     */
    public fun <S : ContractState> retrieveOutput(clazz: Class<S>, label: String): S =
        retrieveOutputStateAndRef(clazz, label).state.data
}
