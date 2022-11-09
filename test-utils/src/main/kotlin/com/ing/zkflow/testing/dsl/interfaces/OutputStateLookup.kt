/*
 * Source attribution:
 *
 * The classes for the ZKFlow test DSL are strongly based on their original non-ZKP counterpart from Corda
 * itself, as defined in the package net.corda.testing.dsl (https://github.com/corda/corda).
 *
 * Ideally ZKFlow could have extended the Corda test DSL to add the ZKP-related parts only, and leave the rest of the behaviour intact.
 * Unfortunately, Corda's test DSL is hard to extend, and it was not possible to add this behaviour without copying most
 * of the original.
 */
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
