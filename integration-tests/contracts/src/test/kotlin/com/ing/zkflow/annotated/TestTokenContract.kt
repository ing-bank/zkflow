package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction

class TestTokenContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "TestTokenContract"
    }

    interface TestTokenStateI : Versioned

    @ZKP
    @BelongsToContract(TestTokenContract::class)
    @Suppress("MagicNumber")
    data class TestTokenState(
        override val owner: @EdDSA AnonymousParty =
            AnonymousParty(PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)),
        val int: Int = 1877,
        val string: @UTF8(10) String = "abc"
    ) : ZKOwnableState, TestTokenStateI {

        override val participants: List<AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            TODO("Not yet implemented")
    }

    interface CreateCommand : Versioned

    // Commands
    @ZKP
    class Create : ZKCommandData, CreateCommand {
        override val metadata: ResolvedZKCommandMetadata
            get() = TODO("Not yet implemented")
    }

    interface MoveCommand : Versioned

    @ZKP
    class Move : ZKCommandData, MoveCommand {
        override val metadata: ResolvedZKCommandMetadata
            get() = TODO("Not yet implemented")
    }

    override fun verify(tx: LedgerTransaction) = Unit
}
