package com.ing.zknotary.common.transactions

import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState

/**
 * A class that defines extension functions to order a list of states by class name.
 * All states of a transaction should be ordered before the transaction gets sent to Zinc.
 * This is because Zinc does not support polymorphism, this implies that List where A is an interface is an invalid input for Zinc.
 * To pass such a list to Zinc, we group the list of states w.r.t. their type;
 * the list will further be split into sublists containing the same type states.
 */
object StateOrdering {
    private val outputComparator = compareBy<TransactionState<*>> { it.data::class.qualifiedName }
    private val inputComparator = compareBy<StateAndRef<*>> { it.state.data::class.qualifiedName }
    private val refComparator = compareBy<ReferencedStateAndRef<*>> { it.stateAndRef.state.data::class.qualifiedName }
    private val utxoComparator = compareBy<UtxoInfo> { it.stateName }

    @JvmName("orderedOutputs")
    fun List<TransactionState<*>>.ordered() = sortedWith(outputComparator)

    @JvmName("orderedInputs")
    fun List<StateAndRef<*>>.ordered() = sortedWith(inputComparator)

    @JvmName("orderedRefs")
    fun List<ReferencedStateAndRef<*>>.ordered() = sortedWith(refComparator)

    @JvmName("orderedUtxoInfos")
    fun List<UtxoInfo>.ordered() = sortedWith(utxoComparator)
}
