package com.example.token.sdk

import net.corda.core.contracts.FungibleState
import net.corda.core.identity.AnonymousParty

// Decomposition replaces the relation: `CBDCToken is FungibleToken`
// with
// `CBDCToken     is AbstractFungibleToken` and
// `FungibleToken is AbstractFungibleToken`
// Such decomposition is required to circumvent the kotlinx.serialization limitation, see
// https://github.com/Kotlin/kotlinx.serialization/issues/1792

abstract class AbstractFungibleToken : FungibleState<IssuedTokenType>, AbstractToken {
    abstract override fun withNewHolder(newHolder: AnonymousParty): AbstractFungibleToken
    override val issuedTokenType: IssuedTokenType get() = amount.token
}
