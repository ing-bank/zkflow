package com.ing.zknotary.common.states

import com.ing.zknotary.common.serializer.SerializationFactoryService
import net.corda.core.KeepForDJVM
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializationFactory
import net.corda.core.serialization.serialize

@KeepForDJVM
@CordaSerializable
data class ZKStateRef(
    val id: SecureHash
) {
    constructor(
        state: ContractState, // TODO: or should this be the TransactionState, that includes the notary info? Tx outputs are TransactionStates...
        serializationFactory: SerializationFactory,
        digestService: DigestService
    ) : this(
        id = digestService.hash(state.serialize(serializationFactory).bytes)
    )

    override fun toString() = id.toString()
}

/** Wrapper over [ZKStateRef] to be used when filtering reference states. */
@KeepForDJVM
@CordaSerializable
data class ZKReferenceStateRef(val zkStateRef: ZKStateRef)

fun ContractState.toZKStateRef(serializationFactoryService: SerializationFactoryService, digestService: DigestService): ZKStateRef {
    return ZKStateRef(this, serializationFactoryService.factory, digestService)
}

fun StateAndRef<ContractState>.toZKStateRef(
    serializationFactoryService: SerializationFactoryService,
    digestService: DigestService
): ZKStateRef {
    return state.data.toZKStateRef(serializationFactoryService, digestService)
}
