package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object LongSerializer : FixedLengthKSerializerWithDefault<Long>, KSerializer<Long> by Long.serializer() {
    override val default = 0.toLong()
    override val descriptor = FixedLengthSerialDescriptor(Long.serializer().descriptor, Long.SIZE_BYTES)
}
