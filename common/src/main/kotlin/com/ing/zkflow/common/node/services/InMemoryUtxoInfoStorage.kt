package com.ing.zkflow.common.node.services

import com.ing.zkflow.common.transactions.UtxoInfo
import net.corda.core.contracts.StateRef
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class InMemoryUtxoInfoStorage(public val serviceHub: AppServiceHub) : WritableUtxoInfoStorage, SingletonSerializeAsToken() {
    private val utxoInfos = mutableMapOf<StateRef, UtxoInfo>()

    override fun addUtxoInfo(utxoInfo: UtxoInfo): Boolean {
        val previous = utxoInfos.putIfAbsent(utxoInfo.stateRef, utxoInfo)
        return previous == null || previous == utxoInfo
    }

    override fun getUtxoInfo(stateRef: StateRef): UtxoInfo? = utxoInfos[stateRef]
}
