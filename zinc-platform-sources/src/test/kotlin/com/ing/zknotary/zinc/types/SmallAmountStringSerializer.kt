package com.ing.zknotary.zinc.types

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SmallStringSurrogate(
    @FixedLength([3])
    val value: String
) : Surrogate<String> {
    override fun toOriginal(): String = value
}

object SmallStringSerializer : KSerializer<String> by (
    SurrogateSerializer(SmallStringSurrogate.serializer()) {
        SmallStringSurrogate(it)
    }
    )
