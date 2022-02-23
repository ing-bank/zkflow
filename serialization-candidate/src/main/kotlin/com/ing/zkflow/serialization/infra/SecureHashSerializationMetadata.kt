package com.ing.zkflow.serialization.infra

import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SecureHashSerializationMetadata(
    @Serializable(with = Algorithm::class) val algorithm: String,
    @Serializable(with = IntSerializer::class) val hashSize: Int,
) {
    object Algorithm : FixedLengthASCIIStringSerializer(MAX_ALGORITHM_LENGTH)

    companion object {
        const val MAX_ALGORITHM_LENGTH: Int = 100
    }
}
