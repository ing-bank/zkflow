package com.ing.zkflow.testing.dsl.interfaces

import net.corda.core.DoNotImplement
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef

/**
 * This interface defines output state lookup by label. It is split from the interpreter interfaces so that outputs may
 * be looked up both in ledger{..} and transaction{..} blocks.
 */
@DoNotImplement
public interface OutputStateLookup {
    /**
     * Retrieves an output previously defined by [ZKTransactionDSLInterpreter.output] with a label passed in.
     * @param clazz The class object holding the type of the output state expected.
     * @param label The label of the to-be-retrieved output state.
     * @return The output [StateAndRef].
     */
    public fun <S : ContractState> retrieveOutputStateAndRef(clazz: Class<S>, label: String): StateAndRef<S>
}
