package com.ing.zkflow.serialization.infra

import com.ing.zkflow.serialization.serializer.IntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class NotarySerializationMetadata(
    @Serializable(with = IntSerializer::class) val notarySignatureSchemeId: Int,
)
