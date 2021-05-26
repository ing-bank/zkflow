package io.ivno.collateraltoken.contract

import io.ivno.collateraltoken.contract.TransferStatus.*
import net.corda.core.serialization.CordaSerializable

/**
 * Specifies the status of a transfer.
 *
 * @property REQUESTED Indicates that a token transfer has been requested.
 * @property ACCEPTED Indicates that a token transfer has been accepted.
 * @property REJECTED Indicates that a token transfer has been rejected.
 * @property CANCELLED Indicates that a token transfer has been cancelled.
 * @property COMPLETED Indicates that a token transfer has completed.
 */
@CordaSerializable
enum class TransferStatus {
    REQUESTED,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    /**
     * Determines whether this [TransferStatus] can advance from the specified [TransferStatus].
     *
     * @param status The transfer status to advance from.
     * @return Returns true if this [TransferStatus] can advance from the specified [TransferStatus]; otherwise false.
     */
    fun canAdvanceFrom(status: TransferStatus): Boolean = when (status) {
        REQUESTED -> this in setOf(ACCEPTED, REJECTED, CANCELLED, COMPLETED)
        ACCEPTED -> this in setOf(CANCELLED, COMPLETED)
        else -> false
    }
}
