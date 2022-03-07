package com.ing.zkflow.serialization.serializer.corda

import com.ing.zkflow.serialization.SerializerTest
import com.ing.zkflow.serialization.engine.SerdeEngine
import com.ing.zkflow.serialization.serializer.IntSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TransactionStateSerializerTest : SerializerTest {
    @Suppress("UNCHECKED_CAST")
    private val strategy = TransactionStateSerializer(
        MockAssetContract.MockAsset.serializer(),
        PartySerializer(
            signatureScheme.schemeNumberID,
            CordaX500NameSerializer
        ),
        SignatureAttachmentConstraintSerializer(
            signatureScheme.schemeNumberID
        ) as KSerializer<AttachmentConstraint>
    )

    @ParameterizedTest
    @MethodSource("engines")
    fun `TransactionState must serialize and deserialize`(engine: SerdeEngine) {
        val state = MockAssetContract.MockAsset.newTxState()
        engine.assertRoundTrip(strategy, state)
    }

    @ParameterizedTest
    @MethodSource("engines")
    fun `TransactionState serializations must have constant size`(engine: SerdeEngine) {
        val state1 = MockAssetContract.MockAsset.newTxState()
        val state2 = MockAssetContract.MockAsset.newTxState()

        engine.serialize(strategy, state1).size shouldBe
            engine.serialize(strategy, state2).size
    }

    private class MockAssetContract : Contract {
        @Serializable
        @BelongsToContract(MockAssetContract::class)
        data class MockAsset(
            @Serializable(with = IntSerializer::class)
            val value: Int = 1024,

            @Serializable(with = Owner::class)
            val owner: AnonymousParty = TestIdentity.fresh("Alice", signatureScheme).party.anonymise()
        ) : ContractState {

            @Transient
            override val participants: List<AnonymousParty> = listOf(owner)

            object Owner : AnonymousPartySerializer(signatureScheme.schemeNumberID)

            companion object {
                fun newTxState(): TransactionState<MockAsset> {
                    val notary = TestIdentity.fresh("Notary", signatureScheme).party

                    return TransactionState(
                        data = MockAsset(),
                        notary = notary,
                        encumbrance = 1,
                        constraint = SignatureAttachmentConstraint(notary.owningKey)
                    )
                }
            }
        }

        override fun verify(tx: LedgerTransaction) {}
    }

    companion object {
        val signatureScheme = Crypto.EDDSA_ED25519_SHA512
    }
}

// class TransactionStateSerializerTest : SerializerTest {
//     // @Suppress("UNCHECKED_CAST")
//     // private val strategy = TransactionStateSerializer1(
//     //     MockAssetContract.MockAsset.serializer()
//     // )
//
//     @ParameterizedTest
//     @MethodSource("engines")
//     fun `TransactionState must serialize and deserialize`(engine: SerdeEngine) {
//         val strategy = TransactionStateSerializer1(
//             TestMe.serializer()
//         )
//
//         engine.assertRoundTrip(strategy, TestMe())
//     }
//
//     @Serializable
//     private data class TestMe(
//         @Serializable(with = IntSerializer::class)
//         val value: Int = 0
//     )
//
//     class TransactionStateSerializer1<T>(
//         innerSerializer: KSerializer<T>
//     ) : SurrogateSerializer<T, TransactionStateSurrogate1<T>>(
//         TransactionStateSurrogate1.serializer(innerSerializer),
//         { TransactionStateSurrogate1(it, "some string") }
//     )
//
//     @Serializable
//     data class TransactionStateSurrogate1<T>(
//         val data: T,
//         @Serializable(with = ContractClassName::class)
//         val contract: String,
//     ) : Surrogate<T> {
//         object ContractClassName : FixedLengthASCIIStringSerializer(100)
//
//         override fun toOriginal() = data
//     }
//
//     // private class MockAssetContract : Contract {
//     //     @Serializable
//     //     @BelongsToContract(MockAssetContract::class)
//     //     data class MockAsset(
//     //         @Serializable(with = IntSerializer::class)
//     //         val value: Int = 1024,
//     //
//     //         @Serializable(with = Owner::class)
//     //         val owner: AnonymousParty = TestIdentity.fresh("Alice", signatureScheme).party.anonymise()
//     //     ) : ContractState {
//     //
//     //         @Transient
//     //         override val participants: List<AnonymousParty> = listOf(owner)
//     //
//     //         object Owner : AnonymousPartySerializer(signatureScheme.schemeNumberID)
//     //
//     //         companion object {
//     //             fun newTxState(): TransactionState<MockAsset> {
//     //                 val notary = TestIdentity.fresh("Notary", signatureScheme).party
//     //
//     //                 return TransactionState(
//     //                     data = MockAsset(),
//     //                     notary = notary,
//     //                     encumbrance = 1,
//     //                     constraint = SignatureAttachmentConstraint(notary.owningKey)
//     //                 )
//     //             }
//     //         }
//     //     }
//     //
//     //     override fun verify(tx: LedgerTransaction) {}
//     // }
//     //
//     // companion object {
//     //     val signatureScheme = Crypto.EDDSA_ED25519_SHA512
//     // }
// }
