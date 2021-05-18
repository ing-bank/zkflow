package com.ing.zknotary.common.contracts

import net.corda.core.KeepForDJVM
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable

@KeepForDJVM
@CordaSerializable
interface ZKContractState : ContractState {

    /**
     * We force usage of AnonymousParty (that is essentially just a PubKey)
     * to get rid of X500Name and produce smaller circuits
     */
    override val participants: List<AnonymousParty>
}
