package com.ing.zkflow.serialization.infra

import com.ing.zkflow.serialization.serializer.IntSerializer
import kotlinx.serialization.Serializable
import net.corda.core.crypto.SecureHash

@Serializable
open class SecureHashSerializationMetadata(
    @Serializable(with = IntSerializer::class) val hashAlgorithmId: Int,
)

val SecureHash.HASH.algorithmId: Int
    get() = algorithm.hashCode()
