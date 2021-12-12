package com.ing.zkflow.annotated.pilot.r3.states

import com.ing.zkflow.annotated.pilot.r3.types.TokenPointer
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

abstract class EvolvableTokenType : LinearState {
    /**
     * The [Party]s which create and maintain this token [EvolvableTokenType]. It probably _is_ the issuer of the token
     * but may not necessarily be. For example, a reference data maintainer may create an [EvolvableTokenType] for
     * some stock, keep all the details up-to-date, and distribute the updates. This [EvolvableTokenType], can
     * then be used by many issuers to create [FungibleToken]s (depository receipts) for the stock in question. Also
     * the actual stock issuer (if they had a Corda node on the network) could use the same stock token to issue ledger
     * native stock.
     */
    abstract val maintainers: List<Party>

    /** Defaults to the maintainer but can be overridden if necessary. */
    override val participants: List<AbstractParty> get() = maintainers

    /**
     * The number of fractional digits allowable for this token type. Specifying "0" will only allow integer amounts of
     * the token type. Specifying "2", allows two decimal places, like most fiat currencies, and so on...
     */
    abstract val fractionDigits: Int

    /** For obtaining a pointer to this [EvolveableTokenType]. */
    inline fun <reified T : EvolvableTokenType> toPointer(): TokenPointer<T> {
        TODO()
    }

    /**
     * For obtaining a pointer to this [EvolveableTokenType].
     *
     * @param tokenTypeClass the [Class] of the [EvolvableTokenType] being pointed to.
     */
    fun <T : EvolvableTokenType> toPointer(tokenTypeClass: Class<T>): TokenPointer<T> {
        val linearPointer = LinearPointer(linearId, tokenTypeClass)
        return TokenPointer(linearPointer, fractionDigits)
    }
}
