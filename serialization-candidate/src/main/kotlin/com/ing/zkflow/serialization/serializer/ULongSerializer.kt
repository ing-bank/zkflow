package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object ULongSerializer : KSerializerWithDefault<ULong>, KSerializer<ULong> by ULong.serializer() {
    override val default: ULong = 0U
}
