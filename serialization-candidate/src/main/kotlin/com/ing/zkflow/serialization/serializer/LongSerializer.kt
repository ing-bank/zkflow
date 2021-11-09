package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object LongSerializer : KSerializerWithDefault<Long>, KSerializer<Long> by Long.serializer() {
    override val default = 0.toLong()
}
