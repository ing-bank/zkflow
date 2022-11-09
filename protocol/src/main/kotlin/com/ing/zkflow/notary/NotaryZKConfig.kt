package com.ing.zkflow.notary

import com.ing.zkflow.common.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.common.zkp.ZKTransactionService

open class NotaryZKConfig(
    val zkTransactionService: ZKTransactionService,
    val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage
)
