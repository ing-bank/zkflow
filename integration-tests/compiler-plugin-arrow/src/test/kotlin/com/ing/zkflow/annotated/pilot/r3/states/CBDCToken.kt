package com.ing.zkflow.annotated.pilot.r3.states

import com.ing.zkflow.Converter
import com.ing.zkflow.Sha256
import com.ing.zkflow.ZKP
import com.ing.zkflow.annotated.pilot.infra.AmountConverter_IssuedTokenType
import com.ing.zkflow.annotated.pilot.infra.AmountSurrogate_IssuedTokenType
import com.ing.zkflow.annotated.pilot.infra.EdDSAAbstractParty
import com.ing.zkflow.annotated.pilot.infra.EdDSAAbstractPartyConverter
import com.ing.zkflow.annotated.pilot.r3.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import java.time.Instant

@ZKP
data class CBDCToken(
    override val amount: @Converter<Amount<IssuedTokenType>, AmountSurrogate_IssuedTokenType>(AmountConverter_IssuedTokenType::class) Amount<IssuedTokenType>,
    override val holder: @Converter<AbstractParty, EdDSAAbstractParty>(EdDSAAbstractPartyConverter::class) AbstractParty,
    override val tokenTypeJarHash: @Sha256 SecureHash? = SecureHash.zeroHash,
    val issueDate: Instant = Instant.now(),
    val lastInterestAccrualDate: Instant = issueDate,
    val usageCount: Int = 0
) : AbstractFungibleToken() { // , ReissuableState<CBDCToken> // this is commented out for simplification, it is just an interface with no fields.
    override fun withNewHolder(newHolder: AbstractParty): AbstractFungibleToken {
        return CBDCToken(amount, newHolder, tokenTypeJarHash = tokenTypeJarHash)
    }
}
