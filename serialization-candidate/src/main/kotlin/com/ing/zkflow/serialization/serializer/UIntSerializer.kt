package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

object UIntSerializer : KSerializerWithDefault<UInt>, KSerializer<UInt> by UInt.serializer() {
    override val default = 0U
}
