package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object BooleanSerializer : FixedLengthKSerializerWithDefault<Boolean>, KSerializer<Boolean> by Boolean.serializer() {
    override val default: Boolean = false

    /**
     * Booleans encoded with 1 byte.
     */
    override val descriptor = FixedLengthSerialDescriptor(Boolean.serializer().descriptor, Byte.SIZE_BYTES)
}
