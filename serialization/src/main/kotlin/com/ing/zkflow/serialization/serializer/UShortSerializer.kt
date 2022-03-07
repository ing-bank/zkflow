package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object UShortSerializer : FixedLengthKSerializerWithDefault<UShort>, KSerializer<UShort> by UShort.serializer() {
    override val default: UShort = 0u
    override val descriptor = FixedLengthSerialDescriptor(UShort.serializer().descriptor, UShort.SIZE_BYTES)
}
