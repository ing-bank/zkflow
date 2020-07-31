package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.node.services.ZKWritableTransactionStorage

open class ZKConfig(
    val zkTransactionService: ZKTransactionService,
    val serializationFactoryService: SerializationFactoryService,
    val zkStorage: ZKWritableTransactionStorage
)
