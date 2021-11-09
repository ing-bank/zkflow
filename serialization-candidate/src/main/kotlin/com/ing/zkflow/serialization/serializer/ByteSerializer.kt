package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object ByteSerializer : KSerializerWithDefault<Byte>, KSerializer<Byte> by Byte.serializer() {
    override val default: Byte = 0
}
