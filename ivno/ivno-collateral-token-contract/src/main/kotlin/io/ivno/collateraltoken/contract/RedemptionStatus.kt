package io.ivno.collateraltoken.contract

import io.ivno.collateraltoken.contract.RedemptionStatus.*
import net.corda.core.serialization.CordaSerializable

/**
 * Specifies the state of a redemption.
 *
 * @property REQUESTED Indicates that a redemption has been requested.
 * @property COMPLETED Indicates that a redemption has been completed.
 * @property REJECTED Indicates that a redemption has been rejected.
 */
@CordaSerializable
enum class RedemptionStatus {
    REQUESTED,
    COMPLETED,
    REJECTED;

    /**
     * Determines whether this [RedemptionStatus] can advance from the specified [RedemptionStatus].
     *
     * @param status The redemption status to advance from.
     * @return Returns true if this [RedemptionStatus] can advance from the specified [RedemptionStatus]; otherwise false.
     */
    fun canAdvanceFrom(status: RedemptionStatus) = when (status) {
        REQUESTED -> this in setOf(COMPLETED, REJECTED)
        else -> false
    }
}
