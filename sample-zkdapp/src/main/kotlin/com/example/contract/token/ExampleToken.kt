package com.example.contract.token

import com.example.token.sdk.AbstractFungibleToken
import com.example.token.sdk.AmountIssuedTokenTypeSurrogate
import com.example.token.sdk.IssuedTokenType
import com.example.token.sdk.TokenType
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
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
    ExampleToken(Amount.fromDecimal(amount, digitalEuroTokenType(issuer)), owner = holder)

interface VersionedExampleToken : VersionedContractStateGroup, ContractState

@CordaSerializable
@BelongsToContract(ExampleTokenContract::class)
@ZKP
data class ExampleToken(
    override val amount: @Via<AmountIssuedTokenTypeSurrogate> Amount<IssuedTokenType>,
    val owner: @EdDSA AnonymousParty
) : AbstractFungibleToken(), VersionedExampleToken {
    override val holder = owner
    override val tokenTypeJarHash: SecureHash = SecureHash.zeroHash

    override fun withNewHolder(newHolder: AnonymousParty): ExampleToken {
        return ExampleToken(amount, newHolder)
    }

    fun withNewHolder(newHolder: AnonymousParty, amount: Double): ExampleToken {
        val decimalAmount = BigDecimal(amount)
        require(decimalAmount <= this.amount.toDecimal()) { "Can't increase amount when assigning a new holder" }
        return ExampleToken(Amount.fromDecimal(decimalAmount, this.amount.token), newHolder)
    }

    fun withNewHolder(newHolder: AnonymousParty, amount: BigDecimal): ExampleToken {
        require(amount <= this.amount.toDecimal()) { "Can't increase amount when assigning a new holder" }
        return ExampleToken(Amount.fromDecimal(amount, this.amount.token), newHolder)
    }

    fun withNewHolder(newHolder: AnonymousParty, amount: Amount<IssuedTokenType>): ExampleToken {
        require(amount <= this.amount) { "Can't increase amount when assigning a new holder" }
        return ExampleToken(amount, newHolder)
    }
}