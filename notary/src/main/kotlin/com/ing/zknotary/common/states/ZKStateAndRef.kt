package com.ing.zknotary.common.states

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.dactyloscopy.Dactyloscopist
import net.corda.core.KeepForDJVM
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.serialization.CordaSerializable

@KeepForDJVM
@CordaSerializable
data class ZKStateAndRef<out T : ContractState>(val state: TransactionState<T>, val ref: ZKStateRef)

fun StateAndRef<ContractState>.toZKStateAndRef(digestService: DigestService): ZKStateAndRef<ContractState> {
    require(state.data is ContractState) { "Contract state must implement ZKContractState" }
    return ZKStateAndRef(state, ZKStateRef(digestService.hash(Dactyloscopist.identify(state))))
}

fun TransactionState<ContractState>.toZKStateAndRef(digestService: DigestService): ZKStateAndRef<ContractState> {
    require(data is ContractState) { "Contract state must implement ZKContractState" }
    val state = this
    return ZKStateAndRef(state, ZKStateRef(digestService.hash(Dactyloscopist.identify(state))))
}

fun <T : CommandData> CommandWithParties<T>.toZKCommand(): Command<ZKCommandData> {
    require(value is ZKCommandData) { "CommandData must implement ZKCommandData" }
    return Command(value as ZKCommandData, signers)
}
