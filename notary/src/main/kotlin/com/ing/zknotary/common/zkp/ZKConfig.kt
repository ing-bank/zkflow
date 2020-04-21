package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serializer.ZKInputSerializer

open class ZKConfig(
    val prover: Prover,
    val verifier: ZKVerifier,
    val serializer: ZKInputSerializer
)