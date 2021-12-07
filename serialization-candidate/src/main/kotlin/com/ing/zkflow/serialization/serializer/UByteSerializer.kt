package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object UByteSerializer : KSerializerWithDefault<UByte>, KSerializer<UByte> by UByte.serializer() {
    override val default: UByte = 0U
}
