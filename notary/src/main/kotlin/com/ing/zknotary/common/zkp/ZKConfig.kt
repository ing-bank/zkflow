package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serializer.SerializationFactoryService

open class ZKConfig(
    val proverService: ZKProverService,
    val verifierService: ZKVerifierService,
    val serializationFactoryService: SerializationFactoryService
)
