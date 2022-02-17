package com.ing.zkflow.testing.fixtures.state

import com.ing.zkflow.testing.fixtures.contract.DummyContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import kotlin.random.Random

@BelongsToContract(DummyContract::class)
public data class DummyState(
    val value: Int, val set: Set<Int>, override val participants: List<AbstractParty>
) : ContractState {
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

        public fun newStateAndRef(notary: Party): StateAndRef<DummyState> {
            return StateAndRef(newTxState(notary), StateRef(SecureHash.randomSHA256(), Random.nextInt(4)))
        }
    }
}
