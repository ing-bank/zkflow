package com.ing.zkflow.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zkflow.serialization.CommandDataSerializerMap
import com.ing.zkflow.serialization.ContractStateSerializerMap
import com.ing.zkflow.serialization.bfl.assertRoundTripSucceeds
import com.ing.zkflow.serialization.bfl.assertSameSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.TransactionState
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import java.util.Random

class TransactionStateTest {
    private val strategy = TransactionStateSerializer(MockAssetContract.MockAsset.serializer())

    @Test
    fun `TransactionState serializes and deserializes`() {
        val state1 = MockAssetContract.MockAsset.newTxState()
        val state2 = MockAssetContract.MockAsset.newTxState()

        assertRoundTripSucceeds(
            state1, strategy = strategy,
            serializers = CordaSerializers.module + SerializersModule {
                contextual(strategy)
            }
        )
        assertSameSize(
            state1, state2, strategy = strategy,
            serializers = CordaSerializers.module + SerializersModule {
                contextual(strategy)
            }
        )
    }
}

val mockSerializers = run {
    ContractStateSerializerMap.register(
        MockAssetContract.MockAsset::class,
        1239993,
        MockAssetContract.MockAsset.serializer()
    )
    CommandDataSerializerMap.register(
        MockAssetContract.Issue::class,
        1239991,
        MockAssetContract.Issue.serializer()
    )
}

private class MockAssetContract : Contract {
    @Serializable
    @BelongsToContract(MockAssetContract::class)
    data class MockAsset(
        val value: Int = Random().nextInt(1000),

        @Serializable(with = AnonymousPartySerializer::class)
        val owner: AnonymousParty = TestIdentity.fresh("Alice").party.anonymise()
    ) : ContractState {

        @FixedLength([1])
        override val participants: List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(owner)

        init {
            // TODO: Hack!
            mockSerializers
        }

        companion object {
            fun newTxState(): TransactionState<MockAsset> {
                val notary = TestIdentity.fresh("Notary")

                return TransactionState(
                    data = MockAsset(),
                    notary = notary.party,
                    encumbrance = 1,
                    constraint = SignatureAttachmentConstraint(notary.publicKey)
                )
            }
        }
    }

    @Serializable
    class Issue : CommandData

    override fun verify(tx: LedgerTransaction) {}
}
