package com.ing.zknotary.testing.fixtures.state

import com.ing.zknotary.testing.fixtures.contract.DealContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
@BelongsToContract(DealContract::class)
public open class DealState(
    public val deal: Deal,
    public val status: Status = Status.PROPOSED,
    public val lastChangedTime: Instant = Instant.now(),
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty>
        get() = listOf(deal.accepter, deal.proposer)

    @CordaSerializable
    public enum class Status {
        PROPOSED,
        ACCEPTED,
        EXPIRED
    }

    public infix fun equalsExceptStatusAndChanged(other: DealState): Boolean =
        deal == other.deal &&
            linearId == other.linearId &&
            status != other.status &&
            lastChangedTime != other.lastChangedTime

    public fun accept(): DealState = DealState(deal = deal, status = Status.ACCEPTED, lastChangedTime = Instant.now())
    public fun expire(): DealState = DealState(deal = deal, status = Status.EXPIRED, lastChangedTime = Instant.now())
}
