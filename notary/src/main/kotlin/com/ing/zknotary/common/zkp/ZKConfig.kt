package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.node.services.ZKWritableProverTransactionStorage
import com.ing.zknotary.node.services.ZKWritableVerifierTransactionStorage

open class ZKConfig(
    val zkTransactionService: ZKTransactionService,
    val serializationFactoryService: SerializationFactoryService,
    val zkProverTransactionStorage: ZKWritableProverTransactionStorage,
    val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage
)
