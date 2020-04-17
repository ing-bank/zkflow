package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.serializer.NoopZKInputSerializer
import com.ing.zknotary.common.serializer.ZKInputSerializer

object DefaultZKConfig : ZKConfig()

open class ZKConfig(
    val prover: Prover = NoopProver(),
    val verifier: Verifier = NoopVerifier(),
    val serializer: ZKInputSerializer = NoopZKInputSerializer
)