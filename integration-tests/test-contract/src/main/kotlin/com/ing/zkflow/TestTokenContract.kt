package com.ing.zkflow

import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.contracts.ZKTransactionMetadataCommandData
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
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

    @ZKP
    @BelongsToContract(TestTokenContract::class)
    @Suppress("MagicNumber")
    data class TestTokenState(
        override val owner: @EdDSA AnonymousParty =
            AnonymousParty(PublicKeySerializer.fixedPublicKey(Crypto.EDDSA_ED25519_SHA512)),
        val int: Int = 1877,
        val string: @ASCII(10) String = "abc"
    ) : ZKOwnableState {

        override val participants: List<AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            TODO("Not yet implemented")
    }

    // Commands
    @ZKP
    class Create : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata
            get() = TODO("Not yet implemented")
        override val metadata: ResolvedZKCommandMetadata
            get() = TODO("Not yet implemented")
    }

    @ZKP
    class Move : ZKTransactionMetadataCommandData {
        override val transactionMetadata: ResolvedZKTransactionMetadata
            get() = TODO("Not yet implemented")
        override val metadata: ResolvedZKCommandMetadata
            get() = TODO("Not yet implemented")
    }

    override fun verify(tx: LedgerTransaction) = Unit
}
