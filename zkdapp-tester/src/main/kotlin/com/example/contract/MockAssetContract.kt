package com.example.contract

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.EdDSA
import com.ing.zkflow.common.contracts.ZKOwnableState
import com.ing.zkflow.common.transactions.zkFLowMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.transactionMetadata
import com.ing.zkflow.serialization.CommandDataSerializerMap
import com.ing.zkflow.serialization.ContractStateSerializerMap
import com.ing.zkflow.serialization.bfl.serializers.AnonymousPartySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import java.util.Random

class MockAssetContract : Contract {
    companion object {
        const val ID: ContractClassName = "com.example.contract.MockAssetContract"
    }

    @ZKP
    @BelongsToContract(MockAssetContract::class)
    data class MockAsset(
        override val owner: @EdDSA AnonymousParty,
        val value: Int = Random().nextInt()
    ) : ZKOwnableState {

        init {
            ContractStateSerializerMap.register(this::class)
        }

        override val participants: List<AnonymousParty> = listOf(owner)

        override fun withNewOwner(newOwner: AnonymousParty): CommandAndState =
            CommandAndState(Move(), copy(owner = newOwner))
    }

    @Serializable
    class Move : ZKTransactionMetadataCommandData {
        init {
            CommandDataSerializerMap.register(this::class)
        }

        override val transactionMetadata by transactionMetadata {
            network { attachmentConstraintType = AlwaysAcceptAttachmentConstraint::class }
            commands {
                +Move::class
            }
        }

        @Transient
        override val metadata = commandMetadata {
            attachmentConstraintType = AlwaysAcceptAttachmentConstraint::class
            numberOfSigners = 2
            inputs { 1 private MockAsset::class }
            outputs { 1 private MockAsset::class }
            timeWindow = true
        }
    }

    @Serializable
    class Issue : ZKTransactionMetadataCommandData {
        init {
            CommandDataSerializerMap.register(this::class)
        }

        override val transactionMetadata by transactionMetadata {
            network { attachmentConstraintType = AlwaysAcceptAttachmentConstraint::class }
            commands {
                +Issue::class
            }
        }

        @Transient
        override val metadata = commandMetadata {
            attachmentConstraintType = AlwaysAcceptAttachmentConstraint::class
            numberOfSigners = 1
            outputs { 1 private MockAsset::class }
            timeWindow = true
        }
    }

    override fun verify(tx: LedgerTransaction) {
        tx.zkFLowMetadata.verify(tx)
    }
}
