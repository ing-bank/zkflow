package com.example.token.cbdc

import com.example.contract.CBDCContract
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.common.versioning.Versioned
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty
import java.time.Instant

@BelongsToContract(CBDCContract::class)
@ZKP
data class CBDCToken (
    override val amount: @Via<AmountSurrogate_IssuedTokenType> Amount<IssuedTokenType>,
    override val holder: @EdDSA AnonymousParty,
    override val tokenTypeJarHash: @SHA256 SecureHash? = SecureHash.zeroHash,
    val issueDate: Instant = Instant.now(),
    val lastInterestAccrualDate: Instant = issueDate,
    val usageCount: Int = 0
) : AbstractFungibleToken(), VersionedCBDCToken { // , ReissuableState<CBDCToken> // this is commented out for simplification, it is just an interface with no fields.
    override fun withNewHolder(newHolder: AnonymousParty): AbstractFungibleToken {
        return CBDCToken(amount, newHolder, tokenTypeJarHash = tokenTypeJarHash)
    }
}

interface VersionedCBDCToken: Versioned
