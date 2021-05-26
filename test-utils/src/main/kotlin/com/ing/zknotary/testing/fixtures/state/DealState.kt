package com.ing.zknotary.testing.fixtures.state

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.zknotary.testing.fixtures.contract.DealContract
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
@Serializable
@BelongsToContract(DealContract::class)
public open class DealState(
    public val deal: @Contextual Deal,
    public val status: Status = Status.PROPOSED,
    public val lastChangedTime: @Contextual Instant = Instant.now(),
    override val linearId: @Contextual UniqueIdentifier = UniqueIdentifier()
) : LinearState {

    @FixedLength([2])
    override val participants: List<AnonymousParty>
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
