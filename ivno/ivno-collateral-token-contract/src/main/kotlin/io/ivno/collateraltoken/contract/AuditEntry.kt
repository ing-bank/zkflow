package io.ivno.collateraltoken.contract

import net.corda.core.serialization.CordaSerializable

/**
 * Represents a meta-data entry which can be used to facilitate an audit trail of a state transition.
 *
 * @property author The author of the meta-data entry.
 * @property message The message of the meta-data entry.
 */
@CordaSerializable
data class AuditEntry(val author: String, val message: String)
