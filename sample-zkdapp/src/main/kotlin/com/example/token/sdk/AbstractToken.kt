/**
 * Source attribution:
 *
 * This class was adapted from the Corda Tokens SDK (https://github.com/corda/token-sdk),
 * to simplify somewhat and use them in tests as a representative example
 * of what we want to be able to support in ZKFlow.
 *
 * They are not part of the ZKFlow distribution, since they are part of a sample application only.
 */
package com.example.token.sdk

import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party

/** Contains common token properties and functionality. */
interface AbstractToken : ContractState {
    /** The [AbstractParty] which is currently holding (some amount of) tokens. */
    val holder: AnonymousParty

    /**
     * The default participant is the current [holder]. However, this can be overridden if required. The standard
     * [FungibleToken] and [NonFungibleToken] states assume that the [holder] is the only participant but they can be
     * sub-classed so an observers list or "CC" list can be added.
     *
     * It is likely that this approach will need to be revisited at the Corda core level, at some point in the near
     * future, as there are some issues with how the participants list interacts with other Corda features, for example
     * notary change transactions and contract upgrade transactions.
     */
    override val participants: List<AbstractParty> get() = listOf(holder)

    /** The [TokenType] this [AbstractToken] is in respect of. */
    val tokenType: TokenType get() = issuedTokenType.tokenType

    /** The [IssuedTokenType]. */
    val issuedTokenType: IssuedTokenType

    /** The issuer [Party]. */
    val issuer: Party get() = issuedTokenType.issuer

    /** For creating a copy of an existing [AbstractToken] with a new holder. */
    fun withNewHolder(newHolder: AnonymousParty): AbstractToken
}
