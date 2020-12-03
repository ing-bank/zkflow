package com.ing.zknotary.notary

import com.ing.zknotary.common.zkp.ZKTransactionService
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage

/**
 * To investigate:
 * Do we need this container at all?
 * Probably we can just inject whatever we need where we need it.
 */
open class NotaryZKConfig(
    val zkTransactionService: ZKTransactionService,
    val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage
)
