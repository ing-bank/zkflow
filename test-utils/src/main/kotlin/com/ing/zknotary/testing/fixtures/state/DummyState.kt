package com.ing.zknotary.testing.fixtures.state

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.testing.fixtures.contract.DummyContract
import com.ing.zknotary.testing.fixtures.contract.DummySerializers
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import kotlin.random.Random

@Serializable
@BelongsToContract(DummyContract::class)
public data class DummyState(
    val value: Int,
    @FixedLength([2]) val set: Set<Int>,
    @FixedLength([2]) override val participants: List<@Polymorphic AbstractParty>
) : ContractState {
    init {
        /*
         * TODO: This is a hack to ensure that the singleton is initialized. In Kotlin they are lazy until accessed.
         */
        DummySerializers
    }

    public companion object {
        public fun any(): DummyState {
            val alice = TestIdentity.fresh("Alice")
            return DummyState(
                Random.nextInt(),
                IntArray(Random.nextInt(1, 3)) { Random.nextInt() }.toSet(),
                listOf(alice.party)
            )
        }

        public fun newTxState(): TransactionState<DummyState> {
            val notary = TestIdentity.fresh("Notary")

            return TransactionState(
                data = any(),
                notary = notary.party,
                encumbrance = 1,
                constraint = HashAttachmentConstraint(SecureHash.zeroHash)
            )
        }

        public fun newTxState(notary: Party): TransactionState<DummyState> {
            return TransactionState(
                data = any(),
                notary = notary,
                encumbrance = 1,
                constraint = HashAttachmentConstraint(SecureHash.zeroHash)
            )
        }
    }
}
