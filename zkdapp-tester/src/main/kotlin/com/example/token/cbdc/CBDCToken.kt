package com.example.token.cbdc

import com.example.contract.CBDCContract
import com.example.token.sdk.AbstractFungibleToken
import com.example.token.sdk.AmountIssuedTokenTypeSurrogate
import com.example.token.sdk.IssuedTokenType
import com.example.token.sdk.TokenType
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.versioning.Versioned
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.time.Instant

val EUR = TokenType("EUR", 2)
fun digitalEuroTokenType(issuer: Party) = IssuedTokenType(issuer, EUR)
fun digitalEuro(amount: Double, issuer: Party, holder: AnonymousParty) = digitalEuro(BigDecimal(amount), issuer, holder)
fun digitalEuro(amount: BigDecimal, issuer: Party, holder: AnonymousParty) =
    CBDCToken(Amount.fromDecimal(amount, digitalEuroTokenType(issuer)), holder = holder)

@BelongsToContract(CBDCContract::class)
@ZKP
data class CBDCToken(
    override val amount: @Via<AmountIssuedTokenTypeSurrogate> Amount<IssuedTokenType>,
    override val holder: @EdDSA AnonymousParty,
    override val tokenTypeJarHash: @SHA256 SecureHash? = SecureHash.zeroHash,
    val issueDate: Instant = Instant.now(),
    val lastInterestAccrualDate: Instant = issueDate,
    val usageCount: Int = 0
) : AbstractFungibleToken(),
    VersionedCBDCToken { // , ReissuableState<CBDCToken> // this is commented out for simplification, it is just an interface with no fields.
    override fun withNewHolder(newHolder: AnonymousParty): AbstractFungibleToken {
        return CBDCToken(amount, newHolder, tokenTypeJarHash = tokenTypeJarHash)
    }
}

interface VersionedCBDCToken : Versioned, ZKContractState
