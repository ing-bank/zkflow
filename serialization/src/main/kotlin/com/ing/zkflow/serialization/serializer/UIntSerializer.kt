package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object UIntSerializer : FixedLengthKSerializerWithDefault<UInt>, KSerializer<UInt> by UInt.serializer() {
    override val default = 0U
    override val descriptor = FixedLengthSerialDescriptor(UInt.serializer().descriptor, UInt.SIZE_BYTES)
}
