package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object UByteSerializer : FixedLengthKSerializerWithDefault<UByte>, KSerializer<UByte> by UByte.serializer() {
    override val default: UByte = 0U
    override val descriptor = FixedLengthSerialDescriptor(UByte.serializer().descriptor, UByte.SIZE_BYTES)
}
