package com.r3.corda.lib.tokens.states

import com.r3.corda.lib.tokens.types.IssuedTokenType
import net.corda.core.contracts.FungibleState

// Decomposition replaces the relation: `ExampleToken is FungibleToken`
// with
// `ExampleToken     is AbstractFungibleToken` and
// `FungibleToken is AbstractFungibleToken`
// Such decomposition is required to circumvent the kotlinx.serialization limitation, see
// https://github.com/Kotlin/kotlinx.serialization/issues/1792

abstract class AbstractFungibleToken : FungibleState<IssuedTokenType>, AbstractToken {
    override val issuedTokenType: IssuedTokenType get() = amount.token
}
