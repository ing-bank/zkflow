package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object ULongSerializer : FixedLengthKSerializerWithDefault<ULong>, KSerializer<ULong> by ULong.serializer() {
    override val default: ULong = 0U
    override val descriptor = FixedLengthSerialDescriptor(ULong.serializer().descriptor, ULong.SIZE_BYTES)
}
