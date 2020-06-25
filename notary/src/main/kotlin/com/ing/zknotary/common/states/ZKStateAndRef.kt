package com.ing.zknotary.common.states

import com.ing.zknotary.common.serializer.SerializationFactoryService
import net.corda.core.KeepForDJVM
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.serialize

@KeepForDJVM
@CordaSerializable
data class ZKStateAndRef<out T : ContractState>(val state: TransactionState<T>, val ref: ZKStateRef)

fun StateAndRef<ContractState>.toZKStateAndRef(
    serializationFactoryService: SerializationFactoryService,
    digestService: DigestService
): ZKStateAndRef<ContractState> {
    return ZKStateAndRef(state, state.toZKStateRef(serializationFactoryService, digestService))
}

fun TransactionState<ContractState>.toZKStateRef(
    serializationFactoryService: SerializationFactoryService,
    digestService: DigestService
): ZKStateRef {
    return ZKStateRef(digestService.hash(this.serialize(serializationFactoryService.factory).bytes))
}

fun TransactionState<ContractState>.toZKStateAndRef(
    serializationFactoryService: SerializationFactoryService,
    digestService: DigestService
): ZKStateAndRef<ContractState> {
    return ZKStateAndRef(this, this.toZKStateRef(serializationFactoryService, digestService))
}
