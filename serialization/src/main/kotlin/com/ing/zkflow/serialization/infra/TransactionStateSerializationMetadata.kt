@file:Suppress("DEPRECATION")
package com.ing.zkflow.serialization.infra

import com.ing.zkflow.serialization.serializer.IntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class TransactionStateSerializationMetadata(
    @Serializable(with = IntSerializer::class) val serializerId: Int,
)
