package com.r3.corda.lib.tokens.annotated.states

import com.ing.zkflow.Via
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.annotations.corda.SHA256
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.r3.corda.lib.tokens.annotated.fixtures.AmountSurrogate_IssuedTokenTypeV1
import com.r3.corda.lib.tokens.annotated.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import java.time.Instant

interface VersionedCBDCToken : VersionedContractStateGroup, ContractState

@ZKP
data class CBDCToken(
    val myAmount: @Via<AmountSurrogate_IssuedTokenTypeV1> Amount<IssuedTokenType>,
    val owner: @EdDSA AnonymousParty,
    val issueDate: Instant = Instant.now(),
    val lastInterestAccrualDate: Instant = issueDate,
    val usageCount: Int = 0
) : AbstractFungibleToken(), VersionedCBDCToken {

    // Fields from parents
    override val amount: Amount<IssuedTokenType> = myAmount
    override val holder: AnonymousParty = owner
    override val tokenTypeJarHash: @SHA256 SecureHash = SecureHash.zeroHash

    override fun withNewHolder(newHolder: AbstractParty): AbstractFungibleToken {
        require(newHolder is AnonymousParty)
        return CBDCToken(myAmount, newHolder, usageCount = usageCount + 1)
    }
}
