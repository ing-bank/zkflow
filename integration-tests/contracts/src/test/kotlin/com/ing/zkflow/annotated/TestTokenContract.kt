package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.OwnableState
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction

class TestTokenContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "TestTokenContract"
    }

    interface TestTokenStateI : VersionedContractStateGroup

    @ZKP
    @BelongsToContract(TestTokenContract::class)
    @Suppress("MagicNumber")
    data class TestTokenState(
        val myOwner: @EdDSA AnonymousParty = AnonymousParty(PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)),
        val int: Int = 1877,
        val string: @UTF8(10) String = "abc"
    ) : OwnableState, TestTokenStateI {
        override val owner: AbstractParty = myOwner

        override val participants: List<AbstractParty> = listOf(owner)

        override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
            TODO("Not yet implemented")
        }
    }

    // Commands
    @ZKP
    class Create : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata
            get() = TODO("Not yet implemented")
    }

    @ZKP
    class Move : ZKCommandData {
        override val metadata: ResolvedZKCommandMetadata
            get() = TODO("Not yet implemented")
    }

    override fun verify(tx: LedgerTransaction) = Unit
}
