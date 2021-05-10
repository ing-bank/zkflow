package io.ivno.collateraltoken.contract

import io.dasl.contracts.v1.token.BigDecimalAmount
import io.ivno.collateraltoken.contract.DepositSchema.DepositSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.security.PublicKey
import java.time.Instant
import java.util.UUID

/**
 * Represents a request to issue tokens for an off-ledger cash deposit.
 *
 * @property depositor The counter-party who is requesting an off-ledger deposit.
 * @property custodian The counter-party who is responsible for maintaining the off-ledger collateral fund.
 * @property tokenIssuingEntity The counter-party who is responsible for signing token issuance and token redemption requests.
 * @property amount An amount of the token type to be deposited.
 * @property reference A unique reference to the off-ledger deposit.
 * @property status The status of the off-ledger deposit.
 * @property timestamp A timestamp representing when this state was created.
 * @property accountId The destination account.
 * @property linearId A [UniqueIdentifier] used to track transition of this state.
 * @property participants A list of [AbstractParty] for which this state is relevant.
 */
@BelongsToContract(DepositContract::class)
data class Deposit internal constructor(
    val depositor: Party,
    val custodian: Party,
    val tokenIssuingEntity: Party,
    val amount: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
    val reference: String?,
    val status: DepositStatus,
    val timestamp: Instant,
    val accountId: String,
    override val linearId: UniqueIdentifier
) : LinearState, QueryableState {

    constructor(
        depositor: Party,
        custodian: Party,
        tokenIssuingEntity: Party,
        amount: BigDecimalAmount<LinearPointer<IvnoTokenType>>,
        accountId: String,
        linearId: UniqueIdentifier = UniqueIdentifier()
    ) : this(
        depositor,
        custodian,
        tokenIssuingEntity,
        amount,
        null,
        DepositStatus.DEPOSIT_REQUESTED,
        Instant.now(),
        accountId,
        linearId
    )

    override val participants: List<AbstractParty> get() = listOf(depositor, custodian, tokenIssuingEntity)

    /**
     * Maps this state to a persistent state.
     */
    override fun generateMappedObject(schema: MappedSchema): PersistentState = when (schema) {
        is DepositSchemaV1 -> DepositSchema.DepositEntity(
            linearId = linearId.id,
            externalId = linearId.externalId,
            depositor = depositor,
            custodian = custodian,
            tokenIssuingEntity = tokenIssuingEntity,
            amount = amount.quantity,
            tokenTypeLinearId = amount.amountType.pointer.id,
            tokenTypeExternalId = amount.amountType.pointer.externalId,
            reference = reference,
            status = status,
            timestamp = timestamp,
            accountId = accountId
        )
        else -> throw IllegalArgumentException("Unrecognised schema: $schema.")
    }

    /**
     * Gets a list of supported state schemas.
     */
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(DepositSchemaV1)

    /**
     * Determines which participants are required to sign the transaction based on the status of the deposit.
     *
     * @return Returns a [List] of [PublicKey] for the participants that are required to sign the transaction.
     */
    fun getRequiredSigningKeys(): List<PublicKey> = when (status) {
        DepositStatus.DEPOSIT_ACCEPTED,
        DepositStatus.DEPOSIT_REJECTED,
        DepositStatus.PAYMENT_REJECTED -> listOf(custodian.owningKey)
        DepositStatus.PAYMENT_ACCEPTED -> listOf(custodian.owningKey, tokenIssuingEntity.owningKey)
        else -> listOf(depositor.owningKey)
    }

    /**
     * Determines which participants are required to be included as counter-parties for deposit transactions.
     *
     * @return Returns a [List] of [Party] representing the transaction counter-parties.
     */
    fun getRequiredCounterparties(): List<Party> = when (status) {
        DepositStatus.DEPOSIT_ACCEPTED,
        DepositStatus.DEPOSIT_REJECTED,
        DepositStatus.PAYMENT_ACCEPTED,
        DepositStatus.PAYMENT_REJECTED -> listOf(depositor, tokenIssuingEntity)
        else -> listOf(custodian, tokenIssuingEntity)
    }

    /**
     * Indicates that the deposit request has been accepted.
     *
     * @param reference A unique reference to the off-ledger deposit.
     * @return Returns a new [Deposit].
     */
    fun acceptDeposit(reference: String = UUID.randomUUID().toString()): Deposit {
        return advance(DepositStatus.DEPOSIT_ACCEPTED).copy(
            reference = reference
        )
    }

    /**
     * Indicates that the deposit request has been rejected.
     *
     * @return Returns a new [Deposit].
     */
    fun rejectDeposit(): Deposit {
        return advance(DepositStatus.DEPOSIT_REJECTED)
    }

    /**
     * Indicates that the deposit request has been cancelled.
     *
     * @return Returns a new [Deposit].
     */
    fun cancelDeposit(): Deposit {
        return advance(DepositStatus.DEPOSIT_CANCELLED)
    }

    /**
     * Indicates that the deposit payment has been issued.
     *
     * @return Returns a new [Deposit].
     */
    fun issuePayment(): Deposit {
        return advance(DepositStatus.PAYMENT_ISSUED)
    }

    /**
     * Indicates that the deposit payment has been accepted.
     *
     * @return Returns a new [Deposit].
     */
    fun acceptPayment(): Deposit {
        return advance(DepositStatus.PAYMENT_ACCEPTED)
    }

    /**
     * Indicates that the deposit payment has been rejected.
     *
     * @return Returns a new [Deposit].
     */
    fun rejectPayment(): Deposit {
        return advance(DepositStatus.PAYMENT_REJECTED)
    }

    /**
     * Checks for equality of immutable properties across states.
     *
     * @param other The other [Deposit] state to compare with this state.
     * @return Returns true if the immutable properties are equal; otherwise, false.
     */
    internal fun immutableEquals(other: Deposit): Boolean {
        return other.depositor == depositor &&
            other.custodian == custodian &&
            other.amount == amount &&
            other.linearId == linearId
    }

    /**
     * Advances the deposit to a new status, provided that the new status can be advanced from the existing status.
     *
     * @param status The [DepositStatus] to advance to.
     * @return Returns a new [Deposit].
     */
    private fun advance(status: DepositStatus): Deposit {
        check(status.canAdvanceFrom(this.status)) {
            "Cannot advance to $status from ${this.status}."
        }

        return copy(status = status, timestamp = Instant.now())
    }
}
