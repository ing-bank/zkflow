package com.ing.zknotary.node.services

import com.ing.zknotary.common.transactions.UtxoInfo
import net.corda.core.DeleteForDJVM
import net.corda.core.DoNotImplement
import net.corda.core.contracts.StateRef
import net.corda.core.serialization.SerializeAsToken

/**
 * Thread-safe storage of UtxoInfos.
 */
@DeleteForDJVM
@DoNotImplement
interface UtxoInfoStorage : SerializeAsToken {
    /**
     * Return the [UtxoInfo] with the given [StateRef], or null if none exists.
     */
    fun getUtxoInfo(stateRef: StateRef): UtxoInfo?
}

/**
 * Thread-safe storage of UtxoInfos.
 */
interface WritableUtxoInfoStorage : UtxoInfoStorage {
    /**
     * Add a new *verified* UtxoInfo to the store. Verified meaning that its history has
     * @param utxoInfo The UtxoInfo to be recorded.
     * @return true if the utxoInfo was recorded as a *new verified* UtxoInfo, false if the UtxoInfo already exists.
     */
    fun addUtxoInfo(utxoInfo: UtxoInfo): Boolean
}
