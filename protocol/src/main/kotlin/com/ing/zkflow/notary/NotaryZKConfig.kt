package com.ing.zkflow.notary

import com.ing.zkflow.common.zkp.ZKTransactionService
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage

/**
 * TODO investigate:
 * Do we need this container at all?
 * Probably we can just inject whatever we need where we need it.
 */
open class NotaryZKConfig(
    val zkTransactionService: ZKTransactionService,
    val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage
)