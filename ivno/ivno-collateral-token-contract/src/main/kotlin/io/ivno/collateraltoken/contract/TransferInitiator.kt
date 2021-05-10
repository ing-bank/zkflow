package io.ivno.collateraltoken.contract

import io.ivno.collateraltoken.contract.TransferInitiator.CURRENT_HOLDER
import io.ivno.collateraltoken.contract.TransferInitiator.TARGET_HOLDER
import net.corda.core.serialization.CordaSerializable

/**
 * Specifies the initiator of a transfer request.
 *
 * @property CURRENT_HOLDER Represents a request to send tokens.
 * @property TARGET_HOLDER Represents a request to receive tokens.
 */
@CordaSerializable
enum class TransferInitiator { CURRENT_HOLDER, TARGET_HOLDER }
