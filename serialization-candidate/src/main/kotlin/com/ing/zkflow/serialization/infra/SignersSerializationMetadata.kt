package com.ing.zkflow.serialization.infra

import com.ing.zkflow.serialization.serializer.IntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SignersSerializationMetadata(
    @Serializable(with = IntSerializer::class) val numberOfSigners: Int,
    @Serializable(with = IntSerializer::class) val participantSignatureSchemeId: Int
)
