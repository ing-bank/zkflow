package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object IntSerializer : FixedLengthKSerializerWithDefault<Int>, KSerializer<Int> by Int.serializer() {
    override val default = 0
    override val descriptor = FixedLengthSerialDescriptor(Int.serializer().descriptor, Int.SIZE_BYTES)
}
