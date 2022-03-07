package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object ByteSerializer : FixedLengthKSerializerWithDefault<Byte>, KSerializer<Byte> by Byte.serializer() {
    override val default: Byte = 0
    override val descriptor = FixedLengthSerialDescriptor(Byte.serializer().descriptor, Byte.SIZE_BYTES)
}
