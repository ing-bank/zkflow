package io.ivno.collateraltoken.contract

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serializers.BigDecimalSizes
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.TransferSchema.TransferSchemaV1
import io.ivno.collateraltoken.serialization.BigDecimalAmountSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.security.PublicKey
import java.time.Instant

/**
 * Represents a token transfer request between two counter-parties.
 *
 * @property currentTokenHolder The counter-party who currently owns the tokens to be transferred.
 * @property targetTokenHolder The counter-party to whom the tokens will be transferred.
 * @property initiator The counter-party who is initiating the transfer request.
 * @property amount An amount of the token type to be transferred.
 * @property status The status of the off-ledger transfer.
 * @property timestamp A timestamp representing when this state was created.
 * @property currentTokenHolderAccountId The origin account.
 * @property targetTokenHolderAccountId The destination account.
 * @property linearId A [UniqueIdentifier] used to track transition of this state.
 * @property participants A list of [AbstractParty] for which this state is relevant.
 */
@BelongsToContract(TransferContract::class)
@Serializable
data class Transfer internal constructor(
    val currentTokenHolder: @Polymorphic AbstractParty,
    val targetTokenHolder: @Polymorphic AbstractParty,
    val initiator: TransferInitiator,
    @Serializable(with = BigDecimalAmountSerializer::class)
    @BigDecimalSizes([AMOUNT_INT_LENGTH, AMOUNT_FRAC_LENGTH])
    val amount: BigDecimalAmount<@Contextual LinearPointer<@Contextual IvnoTokenType>>,
    val status: TransferStatus,
    val timestamp: @Contextual Instant,
    @FixedLength([ACCOUNT_ID_LENGTH])
    val currentTokenHolderAccountId: String,
    @FixedLength([ACCOUNT_ID_LENGTH])
    val targetTokenHolderAccountId: String,
    override val linearId: @Contextual UniqueIdentifier
) : LinearState, QueryableState {
    companion object {
        const val AMOUNT_INT_LENGTH = 20
        const val AMOUNT_FRAC_LENGTH = 4
        const val ACCOUNT_ID_LENGTH = 20
    }

    constructor(
        currentTokenHolder: AbstractParty,
        targetTokenHolder: AbstractParty,
        initiator: TransferInitiator,
        amount: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        currentTokenHolderAccountId: String,
        targetTokenHolderAccountId: String,
        linearId: UniqueIdentifier = UniqueIdentifier()
    ) : this(
        currentTokenHolder,
        targetTokenHolder,
        initiator,
        amount,
        TransferStatus.REQUESTED,
        Instant.now(),
        currentTokenHolderAccountId,
        targetTokenHolderAccountId,
        linearId
    )

    override val participants: List<AbstractParty>
        get() = listOf(currentTokenHolder, targetTokenHolder)

    private val initiatingParty: AbstractParty
        get() = if (initiator == TransferInitiator.CURRENT_HOLDER) currentTokenHolder else targetTokenHolder

    private val nonInitiatingParty: AbstractParty
        get() = if (initiator == TransferInitiator.CURRENT_HOLDER) targetTokenHolder else currentTokenHolder

    /**
     * Maps this state to a persistent state.
     */
    override fun generateMappedObject(schema: MappedSchema): PersistentState = when (schema) {
        is TransferSchemaV1 -> TransferSchema.TransferEntity(
            linearId = linearId.id,
            externalId = linearId.externalId,
            currentTokenHolder = currentTokenHolder,
            targetTokenHolder = targetTokenHolder,
            initiator = initiator,
            amount = amount.quantity,
            tokenTypeLinearId = amount.amountType.pointer.id,
            tokenTypeExternalId = amount.amountType.pointer.externalId,
            status = status,
            timestamp = timestamp,
            currentTokenHolderAccountId = currentTokenHolderAccountId,
            targetTokenHolderAccountId = targetTokenHolderAccountId
        )
        else -> throw IllegalArgumentException("Unrecognised schema: $schema.")
    }

    /**
     * Gets a list of supported state schemas.
     */
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(TransferSchemaV1)

    /**
     * Determines which participant is required to sign the transaction based on the status of the transfer.
     *
     * @returns Returns a [PublicKey] for the participant that is required to sign the transaction.
     */
    fun getRequiredSigningKey(): PublicKey = when (status) {
        TransferStatus.REQUESTED, TransferStatus.CANCELLED -> initiatingParty.owningKey
        TransferStatus.ACCEPTED, TransferStatus.REJECTED -> nonInitiatingParty.owningKey
        TransferStatus.COMPLETED -> currentTokenHolder.owningKey
    }

    /**
     * Determines which participant is required to be included as a counter-party for transfer transactions.
     *
     * @return Returns an [AbstractParty] representing the transaction counter-party.
     */
    fun getRequiredCounterparty(): AbstractParty = when (status) {
        TransferStatus.REQUESTED, TransferStatus.CANCELLED -> nonInitiatingParty
        TransferStatus.ACCEPTED, TransferStatus.REJECTED -> initiatingParty
        TransferStatus.COMPLETED -> targetTokenHolder
    }

    /**
     * Indicates that the transfer has been accepted.
     *
     * @return Returns a new [Transfer].
     */
    fun acceptTransfer(): Transfer {
        return advance(TransferStatus.ACCEPTED)
    }

    /**
     * Indicates that the transfer has been rejected.
     *
     * @return Returns a new [Transfer].
     */
    fun rejectTransfer(): Transfer {
        return advance(TransferStatus.REJECTED)
    }

    /**
     * Indicates that the transfer has been completed and tokens have been transferred.
     *
     * @return Returns a new [Transfer].
     */
    fun completeTransfer(): Transfer {
        return advance(TransferStatus.COMPLETED)
    }

    /**
     * Indicates that the transfer has been cancelled.
     *
     * @return Returns a new [Transfer].
     */
    fun cancelTransfer(): Transfer {
        return advance(TransferStatus.CANCELLED)
    }

    /**
     * Checks for equality of immutable properties across states.
     *
     * @param other The other [Transfer] state to compare with this state.
     * @return Returns true if the immutable properties are equal; otherwise, false.
     */
    internal fun immutableEquals(other: Transfer): Boolean {
        return other.currentTokenHolder == currentTokenHolder &&
        other.targetTokenHolder == targetTokenHolder &&
        other.initiator == initiator &&
        other.amount == amount &&
        other.linearId == linearId
    }

    /**
     * Advances the transfer to a new status, provided that the new status can be advanced from the existing status.
     *
     * @param status The [TransferStatus] to advance to.
     * @return Returns a new [Transfer].
     */
    private fun advance(status: TransferStatus): Transfer {
        check(status.canAdvanceFrom(this.status)) {
            "Cannot advance to $status from ${this.status}."
        }

        return copy(status = status, timestamp = Instant.now())
    }
}
