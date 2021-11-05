package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object ShortSerializer : KSerializerWithDefault<Short>, KSerializer<Short> by Short.serializer() {
    override val default = 0.toShort()
}
