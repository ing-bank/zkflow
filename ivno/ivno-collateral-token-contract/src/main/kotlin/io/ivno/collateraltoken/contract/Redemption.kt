package io.ivno.collateraltoken.contract

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serializers.BigDecimalSizes
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.RedemptionSchema.RedemptionSchemaV1
import io.ivno.collateraltoken.serialization.BigDecimalAmountSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.security.PublicKey
import java.time.Instant

/**
 * Represents a request to redeem tokens for an off-ledger cash withdrawal.
 *
 * @property redeemer The counter-party who is redeeming tokens for an off-ledger cash withdrawal.
 * @property custodian The counter-party with whom the off-ledger deposit has been trusted.
 * @property tokenIssuingEntity The counter-party who is responsible for signing token issuance and token redemption requests.
 * @property amount An amount of the token type to be redeemed.
 * @property status The status of the off-ledger redemption.
 * @property timestamp A timestamp representing when this state was created.
 * @property accountId The destination account.
 * @property linearId A [UniqueIdentifier] used to track transition of this state.
 * @property participants A list of [AbstractParty] for which this state is relevant.
 */
@BelongsToContract(RedemptionContract::class)
@Serializable
data class Redemption internal constructor(
    val redeemer: @Contextual Party,
    val custodian: @Contextual Party,
    val tokenIssuingEntity: @Contextual Party,
    @Serializable(with = BigDecimalAmountSerializer::class)
    @BigDecimalSizes([AMOUNT_INT_LENGTH, AMOUNT_FRAC_LENGTH])
    val amount: BigDecimalAmount<@Contextual LinearPointer<@Contextual IvnoTokenType>>,
    val status: RedemptionStatus,
    val timestamp: @Contextual Instant,
    @FixedLength([ACCOUNT_ID_LENGTH])
    val accountId: String,
    override val linearId: @Contextual UniqueIdentifier
) : LinearState, QueryableState {
    companion object {
        const val AMOUNT_INT_LENGTH = 20
        const val AMOUNT_FRAC_LENGTH = 4
        const val ACCOUNT_ID_LENGTH = 20
    }

    constructor(
        redeemer: Party,
        custodian: Party,
        tokenIssuingEntity: Party,
        amount: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        accountId: String,
        linearId: UniqueIdentifier = UniqueIdentifier()
    ) : this(
        redeemer,
        custodian,
        tokenIssuingEntity,
        amount,
        RedemptionStatus.REQUESTED,
        Instant.now(),
        accountId,
        linearId
    )

    override val participants: List<AbstractParty> get() = listOf(redeemer, custodian, tokenIssuingEntity)

    /**
     * Maps this state to a persistent state.
     */
    override fun generateMappedObject(schema: MappedSchema): PersistentState = when (schema) {
        is RedemptionSchemaV1 -> RedemptionSchema.RedemptionEntity(
            linearId = linearId.id,
            externalId = linearId.externalId,
            redeemer = redeemer,
            custodian = custodian,
            amount = amount.quantity,
            tokenTypeLinearId = amount.amountType.pointer.id,
            tokenTypeExternalId = amount.amountType.pointer.externalId,
            status = status,
            timestamp = timestamp,
            accountId = accountId
        )
        else -> throw IllegalArgumentException("Unrecognised schema: $schema.")
    }

    /**
     * Gets a list of supported state schemas.
     */
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(RedemptionSchemaV1)

    fun getRequiredSigningKeys(): List<PublicKey> = when (status) {
        RedemptionStatus.REQUESTED -> listOf(custodian.owningKey, tokenIssuingEntity.owningKey, redeemer.owningKey)
        RedemptionStatus.COMPLETED -> listOf(custodian.owningKey)
        RedemptionStatus.REJECTED -> listOf(custodian.owningKey, tokenIssuingEntity.owningKey)
    }

    fun getRequiredCounterparties(): List<Party> = when (status) {
        RedemptionStatus.REQUESTED -> listOf(custodian, tokenIssuingEntity)
        RedemptionStatus.COMPLETED, RedemptionStatus.REJECTED -> listOf(redeemer, tokenIssuingEntity)
    }

    /**
     * Indicates that the redemption has been completed.
     *
     * @return Returns a new [Redemption].
     */
    fun completeRedemption(): Redemption {
        return advance(RedemptionStatus.COMPLETED)
    }

    /**
     * Indicates that the redemption has been rejected.
     *
     * @return Returns a new [Redemption].
     */
    fun rejectRedemption(): Redemption {
        return advance(RedemptionStatus.REJECTED)
    }

    /**
     * Checks for equality of immutable properties across states.
     *
     * @param other The other [Redemption] state to compare with this state.
     * @return Returns true if the immutable properties are equal; otherwise, false.
     */
    internal fun immutableEquals(other: Redemption): Boolean {
        return other.redeemer == redeemer &&
            other.custodian == custodian &&
            other.tokenIssuingEntity == tokenIssuingEntity &&
            other.amount == amount &&
            other.linearId == linearId
    }

    /**
     * Advances the redemption to a new status, provided that the new status can be advanced from the existing status.
     *
     * @param status The [RedemptionStatus] to advance to.
     * @return Returns a new [Redemption].
     */
    private fun advance(status: RedemptionStatus): Redemption {
        check(status.canAdvanceFrom(this.status)) {
            "Cannot advance to $status from ${this.status}."
        }

        return copy(status = status, timestamp = Instant.now())
    }
}
