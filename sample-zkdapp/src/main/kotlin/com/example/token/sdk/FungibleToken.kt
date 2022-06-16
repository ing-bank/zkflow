package com.example.token.sdk

import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.common.versioning.Versioned
import net.corda.core.contracts.Amount
import net.corda.core.contracts.FungibleState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty

// Decomposition replaces the relation: `CBDCToken is FungibleToken`
// with
// `CBDCToken     is AbstractFungibleToken` and
// `FungibleToken is AbstractFungibleToken`
// Such decomposition is required to circumvent the kotlinx.serialization limitation, see
// https://github.com/Kotlin/kotlinx.serialization/issues/1792

@ZKP
data class FungibleToken constructor(
    override val amount: @Via<AmountIssuedTokenTypeSurrogate> Amount<IssuedTokenType>,
    override val holder: @EdDSA AnonymousParty,
    override val tokenTypeJarHash: @SHA256 SecureHash? = SecureHash.zeroHash
) : AbstractFungibleToken(), VersionedFungibleToken {
    override fun withNewHolder(newHolder: AnonymousParty): FungibleToken {
        return FungibleToken(amount, newHolder, tokenTypeJarHash = tokenTypeJarHash)
    }
}

abstract class AbstractFungibleToken : FungibleState<IssuedTokenType>, AbstractToken {
    abstract override fun withNewHolder(newHolder: AnonymousParty): AbstractFungibleToken
    override val issuedTokenType: IssuedTokenType get() = amount.token
}

interface VersionedFungibleToken : Versioned
