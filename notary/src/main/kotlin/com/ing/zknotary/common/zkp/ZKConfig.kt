package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serializer.SerializationFactoryService

open class ZKConfig(
    val prover: Prover,
    val verifier: ZKVerifier,
    val serializationFactoryService: SerializationFactoryService
)
