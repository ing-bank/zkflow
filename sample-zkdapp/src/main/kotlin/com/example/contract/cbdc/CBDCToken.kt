package com.example.contract.cbdc

import com.example.token.sdk.AbstractFungibleToken
import com.example.token.sdk.AmountIssuedTokenTypeSurrogate
import com.example.token.sdk.IssuedTokenType
import com.example.token.sdk.TokenType
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal

val EUR = TokenType("EUR", 2)
fun digitalEuroTokenType(issuer: Party) = IssuedTokenType(issuer, EUR)
fun digitalEuro(amount: Double, issuer: Party, holder: AnonymousParty) = digitalEuro(BigDecimal(amount), issuer, holder)
fun digitalEuro(amount: BigDecimal, issuer: Party, holder: AnonymousParty) =
    CBDCToken(Amount.fromDecimal(amount, digitalEuroTokenType(issuer)), owner = holder)

interface VersionedCBDCToken : VersionedContractStateGroup, ContractState

@CordaSerializable
@BelongsToContract(CBDCContract::class)
@ZKP
data class CBDCToken(
    override val amount: @Via<AmountIssuedTokenTypeSurrogate> Amount<IssuedTokenType>,
    val owner: @EdDSA AnonymousParty
) : AbstractFungibleToken(), VersionedCBDCToken {
    override val holder = owner
    override val tokenTypeJarHash: SecureHash = SecureHash.zeroHash

    override fun withNewHolder(newHolder: AnonymousParty): CBDCToken {
        return CBDCToken(amount, newHolder)
    }

    fun withNewHolder(newHolder: AnonymousParty, amount: Double): CBDCToken {
        val decimalAmount = BigDecimal(amount)
        require(decimalAmount <= this.amount.toDecimal()) { "Can't increase amount when assigning a new holder" }
        return CBDCToken(Amount.fromDecimal(decimalAmount, this.amount.token), newHolder)
    }

    fun withNewHolder(newHolder: AnonymousParty, amount: BigDecimal): CBDCToken {
        require(amount <= this.amount.toDecimal()) { "Can't increase amount when assigning a new holder" }
        return CBDCToken(Amount.fromDecimal(amount, this.amount.token), newHolder)
    }

    fun withNewHolder(newHolder: AnonymousParty, amount: Amount<IssuedTokenType>): CBDCToken {
        require(amount <= this.amount) { "Can't increase amount when assigning a new holder" }
        return CBDCToken(amount, newHolder)
    }
}