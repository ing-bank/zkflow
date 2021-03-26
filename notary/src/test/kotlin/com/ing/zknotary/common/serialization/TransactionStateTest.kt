package com.ing.zknotary.common.serialization

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.common.serialization.bfl.corda.CordaSignatureSchemeToSerializers
import com.ing.zknotary.common.serialization.bfl.corda.TransactionStateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import kotlin.random.Random

@ExperimentalSerializationApi
class TransactionStateTest {
    private val localSerializers = CordaSignatureSchemeToSerializers.serializersModuleFor(Crypto.DEFAULT_SIGNATURE_SCHEME)
    private val strategy = TransactionStateSerializer(DummyContract.State.serializer())

    class DummyContract : Contract {
        override fun verify(tx: LedgerTransaction) = TODO("Dummy verification should never be called")

        @Serializable
        data class State(
            val value: Int,
            @FixedLength([2]) val set: Set<Int>,
            @FixedLength([2]) override val participants: List<@Contextual AbstractParty>
        ) : ContractState
    }

    @Test
    fun `TransactionState serializes and deserializes`() {
        val state1 = makeState()
        val state2 = makeState()

        roundTrip(state1, localSerializers, strategy)
        sameSize(state1, state2, localSerializers, strategy)
    }

    private fun makeState(): TransactionState<DummyContract.State> {
        val alice = TestIdentity.fresh("Alice")
        val notary = TestIdentity.fresh("Notary")
        val state = DummyContract.State(
            Random.nextInt(),
            IntArray(Random.nextInt(1, 3)) { Random.nextInt() }.toSet(),
            listOf(alice.party)
        )

        return TransactionState(
            data = state,
            notary = notary.party,
            encumbrance = 1,
            // constraint = AutomaticPlaceholderConstraint // Fails because BFL ElementFactory.fromType has no when clause for isObject
            constraint = HashAttachmentConstraint(SecureHash.zeroHash)
        )
    }
}
