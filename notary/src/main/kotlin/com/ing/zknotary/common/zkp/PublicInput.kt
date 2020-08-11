package com.ing.zknotary.common.zkp

import net.corda.core.crypto.SecureHash

data class PublicInput(
    /**
     * The id of the transaction that is being verified.
     */
    val transactionId: SecureHash
)
