package io.ivno.collateraltoken.contract

import io.ivno.collateraltoken.contract.DepositStatus.*
import net.corda.core.serialization.CordaSerializable

/**
 * Specifies the status of a deposit.
 *
 * @property DEPOSIT_REQUESTED Indicates that the depositor is requesting to make an off-ledger deposit.
 * @property DEPOSIT_ACCEPTED Indicates that the custodian has accepted the depositor's request to make an off-ledger deposit.
 * @property DEPOSIT_REJECTED Indicates that the custodian has rejected the depositor's request to make an off-ledger deposit.
 * @property DEPOSIT_CANCELLED Indicates that the depositor has cancelled the deposit.
 * @property PAYMENT_ISSUED Indicates that the depositor has issued an off-ledger deposit.
 * @property PAYMENT_ACCEPTED Indicates that the custodian has accepted the depositor's off-ledger deposit.
 * @property PAYMENT_REJECTED Indicates that the custodian has rejected the the depositor's off-ledger deposit.
 */
@CordaSerializable
enum class DepositStatus {
    DEPOSIT_REQUESTED,
    DEPOSIT_ACCEPTED,
    DEPOSIT_REJECTED,
    DEPOSIT_CANCELLED,
    PAYMENT_ISSUED,
    PAYMENT_ACCEPTED,
    PAYMENT_REJECTED;

    /**
     * Determines whether this [DepositStatus] can advance from the specified [DepositStatus].
     *
     * @param status The deposit status to advance from.
     * @return Returns true if this [DepositStatus] can advance from the specified [DepositStatus]; otherwise false.
     */
    fun canAdvanceFrom(status: DepositStatus): Boolean = when (status) {
        DEPOSIT_REQUESTED -> this in setOf(DEPOSIT_ACCEPTED, DEPOSIT_REJECTED, DEPOSIT_CANCELLED)
        DEPOSIT_ACCEPTED -> this in setOf(PAYMENT_ISSUED, DEPOSIT_CANCELLED)
        DEPOSIT_REJECTED -> this in setOf(DEPOSIT_REQUESTED, DEPOSIT_CANCELLED)
        PAYMENT_ISSUED -> this in setOf(PAYMENT_ACCEPTED, PAYMENT_REJECTED, DEPOSIT_CANCELLED)
        PAYMENT_REJECTED -> this in setOf(PAYMENT_ISSUED, DEPOSIT_CANCELLED)
        else -> false
    }
}
