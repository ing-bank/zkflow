package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object ShortSerializer : FixedLengthKSerializerWithDefault<Short>, KSerializer<Short> by Short.serializer() {
    override val default = 0.toShort()
    override val descriptor = FixedLengthSerialDescriptor(Short.serializer().descriptor, Short.SIZE_BYTES)
}
