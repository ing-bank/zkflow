package com.ing.zkflow.annotated.pilot.r3.states

import com.ing.zkflow.Converter
import com.ing.zkflow.Default
import com.ing.zkflow.ZKP
import com.ing.zkflow.annotated.pilot.infra.AmountConverter_IssuedTokenType
import com.ing.zkflow.annotated.pilot.infra.AmountSurrogate_IssuedTokenType
import com.ing.zkflow.annotated.pilot.infra.EdDSAAbstractParty
import com.ing.zkflow.annotated.pilot.infra.EdDSAAbstractPartyConverter
import com.ing.zkflow.annotated.pilot.infra.SecureHashConverter_SHA256
import com.ing.zkflow.annotated.pilot.infra.SecureHashSHA256DefaultProvider
import com.ing.zkflow.annotated.pilot.infra.SecureHashSHA256Surrogate
import com.ing.zkflow.annotated.pilot.r3.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.FungibleState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

// Decomposition replaces the relation: `CBDCToken is FungibleToken`
// with
// `CBDCToken     is AbstractFungibleToken` and
// `FungibleToken is AbstractFungibleToken`
// Such decomposition is required to circumvent the kotlinx.serialization limitation, see
// https://github.com/Kotlin/kotlinx.serialization/issues/1792

@ZKP
data class FungibleToken constructor(
    override val amount: @Converter<Amount<IssuedTokenType>, AmountSurrogate_IssuedTokenType>(
        AmountConverter_IssuedTokenType::class
    ) Amount<IssuedTokenType>,
    override val holder: @Converter<AbstractParty, EdDSAAbstractParty>(EdDSAAbstractPartyConverter::class) AbstractParty,
    override val tokenTypeJarHash:
        @Default<SecureHash>(SecureHashSHA256DefaultProvider::class)
        @Converter<SecureHash, SecureHashSHA256Surrogate>(SecureHashConverter_SHA256::class)
        SecureHash? = SecureHash.zeroHash
) : AbstractFungibleToken() {
    override fun withNewHolder(newHolder: AbstractParty): FungibleToken {
        return FungibleToken(amount, newHolder, tokenTypeJarHash = tokenTypeJarHash)
    }
}

abstract class AbstractFungibleToken : FungibleState<IssuedTokenType>, AbstractToken, QueryableState {
    //
    override val issuedTokenType: IssuedTokenType get() = amount.token
    //
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        TODO()
    }
    //
    override fun supportedSchemas() = emptyList<MappedSchema>()
}
