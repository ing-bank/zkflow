package com.ing.zknotary.common.contracts

import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty

/**
 * Clone of OwnableState that only allows AnonymousParty as an owner
 */
interface ZKOwnableState : ZKContractState, OwnableState {

    /**
     * We force usage of AnonymousParty (that is essentially just a PubKey)
     * to get rid of X500Name and produce smaller circuits
     */
    override val owner: AnonymousParty

    fun withNewOwner(newOwner: AnonymousParty): CommandAndState

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState = error("Can't use AbstractParty as an owner")
}
