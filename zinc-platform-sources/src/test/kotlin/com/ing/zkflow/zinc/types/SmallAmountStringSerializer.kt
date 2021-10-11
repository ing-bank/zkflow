package com.ing.zkflow.zinc.types

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SmallStringSurrogate(
    @FixedLength([3])
    val value: String
) : Surrogate<String> {
    override fun toOriginal(): String = value
}

object SmallStringSerializer : SurrogateSerializer<String, SmallStringSurrogate>(
    SmallStringSurrogate.serializer(),
    { SmallStringSurrogate(it) }
)
